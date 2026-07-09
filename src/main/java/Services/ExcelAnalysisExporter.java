package Services;

import Classes.AnalysisResultData;
import Classes.FileAnalysisData;
import Classes.AnalysisSettings;
import Interfaces.IService;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ExcelAnalysisExporter implements IService {

    private static final String[] TYPE_ORDER =
            {"KEYWORD", "IDENTIFIER", "LITERAL", "OPERATOR", "SEPARATOR", "COMMENT"};

    /**
     * Запускает анализ + запись в Excel в фоновом потоке.
     * onProgress вызывается из фонового потока с числом обработанных файлов.
     * onFinished вызывается один раз по завершении: null при успехе, исключение при ошибке.
     * Оба коллбэка не переносят выполнение на FX-поток сами - это обязан сделать вызывающий код.
     */
    public void exportAsync(List<File> selected, AnalysisSettings settings, File targetFile,
                            IntConsumer onProgress, Consumer<Exception> onFinished) {
        Thread worker = new Thread(() -> {
            try {
                export(selected, settings, targetFile, onProgress);
                onFinished.accept(null);
            } catch (Exception e) {
                onFinished.accept(e);
            }
        }, "excel-export-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void export(List<File> selected, AnalysisSettings settings, File targetFile,
                        IntConsumer onProgress) throws Exception {

        // Уже учитывает лимит в 100 файлов - см. SettingsService.getSettings(filesCount)
        boolean perFileSheets = settings.showFileDetails;

        var aggregate = new AnalysisResultData();
        aggregate.setFiles(selected);
        Set<TokenType> selectedTypes = Set.copyOf(settings.selectedTokenTypes);

        // Заполняется только если нужны страницы по файлам (то есть файлов <=100) -
        // в этом случае держать все FileAnalysisData в памяти одновременно тривиально дёшево
        Map<File, FileAnalysisData> perFileData = perFileSheets
                ? Collections.synchronizedMap(new LinkedHashMap<>())
                : null;

        int total = selected.size();
        AtomicInteger completed = new AtomicInteger(0);

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (File file : selected) {
            futures.add(executor.submit(() -> {
                analyzeFileForExport(file, selectedTypes, aggregate, perFileData);
                int done = completed.incrementAndGet();
                if (onProgress != null) onProgress.accept(done);
            }));
        }

        executor.shutdown();
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                throw new Exception("Ошибка анализа файла: " + e.getCause().getMessage(), e.getCause());
            }
        }

        // Запись в Excel - строго однопоточно, POI Workbook не потокобезопасен
        writeWorkbook(aggregate, perFileData, settings, targetFile);
    }

    private void analyzeFileForExport(File file, Set<TokenType> selectedTypes,
                                      AnalysisResultData aggregate,
                                      Map<File, FileAnalysisData> perFileData) {
        try {
            var lexer = new RegexLexer(new JavaLanguage());
            var code = Files.readString(file.toPath());
            var tokens = lexer.tokenize(code);

            var fileData = new FileAnalysisData(file);
            fileData.lineCount = (int) code.lines().count();
            fileData.nonEmptyLineCount = (int) code.lines().filter(l -> !l.strip().isEmpty()).count();

            for (Token token : tokens) {
                aggregate.addTokenType(token.getType());
                fileData.tokenTypeCount.merge(token.getType(), 1, Integer::sum);
            }

            for (Token token : tokens) {
                if (!selectedTypes.contains(token.getType())) continue;
                aggregate.addLexeme(token.getType(), token.getValue());
                fileData.tokenCount
                        .computeIfAbsent(token.getType(), k -> new HashMap<>())
                        .merge(token.getValue(), 1, Integer::sum);
            }

            aggregate.addLines(fileData.lineCount, fileData.nonEmptyLineCount);

            // Сырые "tokens" после этого метода становятся недостижимы и уходят под GC.
            // Если нужен список токенов - он будет пересчитан заново при записи страницы файла.
            if (perFileData != null) {
                perFileData.put(file, fileData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Файл " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    // ==== Запись книги ====

    private void writeWorkbook(AnalysisResultData aggregate, Map<File, FileAnalysisData> perFileData,
                               AnalysisSettings settings, File targetFile) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeGeneralSheet(workbook, aggregate);

            if (perFileData != null) {
                Set<String> usedNames = new HashSet<>();
                for (Map.Entry<File, FileAnalysisData> entry : perFileData.entrySet()) {
                    String sheetName = uniqueSheetName(entry.getKey().getName(), usedNames);
                    writeFileSheet(workbook, sheetName, entry.getValue(), settings);
                }
            }

            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                workbook.write(out);
            }
        }
    }

    // ==== Страница общей статистики (тот же формат, что в обычном компактном выводе) ====

    private void writeGeneralSheet(Workbook workbook, AnalysisResultData aggregate) {
        Sheet sheet = workbook.createSheet("Общая информация");
        int[] rowIdx = {0};

        appendRow(sheet, rowIdx, "Файлов: " + aggregate.getFiles().size());
        appendRow(sheet, rowIdx, "Строк: " + aggregate.getLineCount()
                + " (непустых: " + aggregate.getNonEmptyLineCount() + ")");

        long totalTokens = aggregate.getTokenTypeCount().values().stream()
                .mapToLong(LongAdder::sum).sum();
        appendRow(sheet, rowIdx, "Токенов всего: ", totalTokens);
        rowIdx[0]++;

        appendRow(sheet, rowIdx, "По типам:");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            LongAdder counter = aggregate.getTokenTypeCount().get(type);
            long count = counter == null ? 0 : counter.sum();
            if (count > 0) appendRow(sheet, rowIdx, "  " + type + ": ", count);
        }

        if (!aggregate.getTokenCount().isEmpty()) {
            rowIdx[0]++;
            appendLexemeListConcurrent(sheet, rowIdx, aggregate.getTokenCount());
        }

        sheet.autoSizeColumn(0);
    }

    // ==== Страница одного файла (та же логика, что в общем выводе) ====

    private void writeFileSheet(Workbook workbook, String sheetName, FileAnalysisData fileData,
                                AnalysisSettings settings) throws Exception {
        Sheet sheet = workbook.createSheet(sheetName);
        int[] rowIdx = {0};

        appendRow(sheet, rowIdx, "Файл: " + fileData.file.getName());
        appendRow(sheet, rowIdx, "Строк: " + fileData.lineCount
                + " (непустых: " + fileData.nonEmptyLineCount + ")");

        long totalTokens = fileData.tokenTypeCount.values().stream().mapToLong(Integer::longValue).sum();
        appendRow(sheet, rowIdx, "Токенов всего: " + totalTokens);
        rowIdx[0]++;

        appendRow(sheet, rowIdx, "По типам:");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            int count = fileData.tokenTypeCount.getOrDefault(type, 0);
            if (count > 0) appendRow(sheet, rowIdx, "  " + type + ": ", count);
        }

        if (!fileData.tokenCount.isEmpty()) {
            rowIdx[0]++;
            appendLexemeList(sheet, rowIdx, fileData.tokenCount);
        }

        if (settings.showTokenList) {
            rowIdx[0]++;
            writeTokenListSection(sheet, rowIdx, fileData.file);
        }

        sheet.autoSizeColumn(0);
    }

    // Список токенов "как их вернул лексер" - тип и значение подряд, каждый на своей строке.
    // Файл читается и токенизируется заново здесь (а не хранится с первого прохода) -
    // это допустимо, т.к. страницы по файлам создаются только когда файлов <=100.
    private void writeTokenListSection(Sheet sheet, int[] rowIdx, File file) throws Exception {
        appendRow(sheet, rowIdx, "Список токенов:");

        var lexer = new RegexLexer(new JavaLanguage());
        var code = Files.readString(file.toPath());
        var tokens = lexer.tokenize(code);

        for (Token token : tokens) {
            appendRow(sheet, rowIdx, token.getType().toString(), token.getValue());
        }
    }

    // ==== Хелперы форматирования ====

    private void appendLexemeList(Sheet sheet, int[] rowIdx, Map<TokenType, Map<String, Integer>> groups) {
        for (String typeName : TYPE_ORDER) {
            TokenType type;
            try { type = TokenType.valueOf(typeName); }
            catch (IllegalArgumentException e) { continue; }
            if (!groups.containsKey(type)) continue;

            var values = groups.get(type);
            int typeTotal = values.values().stream().mapToInt(Integer::intValue).sum();
            appendRow(sheet, rowIdx, type + " (" + typeTotal + " шт.):");

            values.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> appendRow(sheet, rowIdx, "  " + e.getKey() + ": ", e.getValue()));
            rowIdx[0]++;
        }
    }

    private void appendLexemeListConcurrent(Sheet sheet, int[] rowIdx,
                                            Map<TokenType, ConcurrentHashMap<String, LongAdder>> groups) {
        for (String typeName : TYPE_ORDER) {
            TokenType type;
            try { type = TokenType.valueOf(typeName); }
            catch (IllegalArgumentException e) { continue; }
            if (!groups.containsKey(type)) continue;

            var values = groups.get(type);
            long typeTotal = values.values().stream().mapToLong(LongAdder::sum).sum();
            appendRow(sheet, rowIdx, type + " (" + typeTotal + " шт.):");

            values.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                    .forEach(e -> appendRow(sheet, rowIdx, "  " + e.getKey() + ": " + e.getValue().sum()));
            rowIdx[0]++;
        }
    }

    private void appendRow(Sheet sheet, int[] rowIdx, Object... values) {
        Row row = sheet.createRow(rowIdx[0]++);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            var cell = row.createCell(i);

            if (value == null) {
                cell.setCellValue("");
            } else if (value instanceof Number num) {
                // Числа сохраняем как числа (работает с Integer, Long, Double, Float и т.д.)
                cell.setCellValue(num.doubleValue());
            } else if (value instanceof Boolean bool) {
                cell.setCellValue(bool);
            } else {
                // Всё остальное как текст
                cell.setCellValue(value.toString());
            }
        }
    }

    // Имена листов Excel: максимум 31 символ, без \ / ? * [ ] : , и должны быть уникальны в книге
    private String uniqueSheetName(String rawName, Set<String> usedNames) {
        String cleaned = rawName.replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (cleaned.length() > 31) cleaned = cleaned.substring(0, 31);
        if (cleaned.isBlank()) cleaned = "file";

        String candidate = cleaned;
        int suffix = 1;
        while (usedNames.contains(candidate)) {
            String suffixStr = "_" + suffix;
            int maxBaseLen = 31 - suffixStr.length();
            String base = cleaned.length() > maxBaseLen ? cleaned.substring(0, maxBaseLen) : cleaned;
            candidate = base + suffixStr;
            suffix++;
        }
        usedNames.add(candidate);
        return candidate;
    }
}