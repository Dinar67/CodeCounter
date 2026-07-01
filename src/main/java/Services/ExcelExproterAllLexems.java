package Services;

import Interfaces.IService;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelExproterAllLexems implements IService {
    public ExcelExproterAllLexems(){}

    public void exportWorkbook(String outputPath, boolean sorted, Map<File, List<Token>> fileTokensMap) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        buildSummarySheet(workbook, fileTokensMap); // Общая информация
        for (Map.Entry<File, List<Token>> entry : fileTokensMap.entrySet()) {
            buildFileSheet(workbook, entry.getKey(), entry.getValue(), sorted); // Для каждого файла своя информация
        }
        autoSizeAllSheets(workbook);
        FileOutputStream fos = new FileOutputStream(outputPath);
        workbook.write(fos);
        workbook.close();
        fos.close();
    }



    // Лист 1: общая статистика + все токены с частотой использования
    private void buildSummarySheet(Workbook workbook, Map<File, List<Token>> fileTokensMap) {
        Sheet sheet = workbook.createSheet("Общая статистика");
        int row = 0;

        List<Token> all = fileTokensMap.values().stream()
                .flatMap(Collection::stream)
                .filter(t -> t.getType() != TokenType.WHITESPACE)
                .collect(Collectors.toList()); // Фильтр токенов, все кроме пробелов

        row = writeStatsSection(sheet, row, all);
        row++; // пустая строка между секциями
        writeTokenFrequencyTable(sheet, row, all);
    }

    // Лист на каждый файл: статистика файла + список токенов
    private void buildFileSheet(Workbook workbook, File file, List<Token> tokens, boolean sorted) {
        String sheetName = uniqueSheetName(workbook, file.getName());
        Sheet sheet = workbook.createSheet(sheetName);
        int row = 0;

        List<Token> filtered = tokens.stream()
                .filter(t -> t.getType() != TokenType.WHITESPACE)
                .collect(Collectors.toList());

        row = writeStatsSection(sheet, row, filtered);
        row++;
        writeTokenInstanceTable(sheet, row, filtered, sorted);
    }
    private String uniqueSheetName(Workbook workbook, String name) {
        // Excel: максимум 31 символ
        String base = name.length() > 31 ? name.substring(0, 28) + "..." : name;

        if (workbook.getSheet(base) == null) {
            return base;
        }

        // Добавляем счётчик пока имя не станет уникальным: "Test.java (2)"
        int counter = 2;
        while (true) {
            String candidate = base + " (" + counter + ")";
            // Укорачиваем если с суффиксом вышло больше 31 символа
            if (candidate.length() > 31) {
                candidate = base.substring(0, 31 - (" (" + counter + ")").length()) + " (" + counter + ")";
            }
            if (workbook.getSheet(candidate) == null) {
                return candidate;
            }
            counter++;
        }
    }



    /**
     * Таблица статистики: тип токена → количество.
     * Возвращает следующий свободный номер строки.
     */
    private int writeStatsSection(Sheet sheet, int startRow, List<Token> tokens) {
        int row = startRow;
        createRow(sheet, row++, "Тип токена", "Количество");

        Map<TokenType, Integer> counter = countByType(tokens);
        for (TokenType type : relevantTypes()) {
            int count = counter.getOrDefault(type, 0);
            if (count > 0) {
                createRow(sheet, row++, type.toString(), String.valueOf(count));
            }
        }
        return row;
    }

    /**
     * Сводная таблица уникальных токенов с частотой.
     * Используется только на сводном листе.
     */
    private void writeTokenFrequencyTable(Sheet sheet, int startRow, List<Token> tokens) {
        int row = startRow;
        createRow(sheet, row++, "№", "Значение", "Тип токена", "Кол-во использований");

        // Группируем по (тип, значение) → считаем частоту
        Map<TokenType, Map<String, Long>> grouped = tokens.stream()
                .collect(Collectors.groupingBy(
                        Token::getType,
                        Collectors.groupingBy(Token::getValue, Collectors.counting())
                ));

        int num = 1;
        for (TokenType type : relevantTypes()) {
            Map<String, Long> byValue = grouped.getOrDefault(type, Collections.emptyMap());
            for (Map.Entry<String, Long> entry : byValue.entrySet()) {
                createRow(sheet, row++,
                        String.valueOf(num++),
                        entry.getKey(),
                        type.toString(),
                        String.valueOf(entry.getValue()));
            }
        }
    }

    /**
     * Список токенов файла с порядковым номером.
     * Используется на листах отдельных файлов.
     */
    private void writeTokenInstanceTable(Sheet sheet, int startRow, List<Token> tokens, boolean sorted) {
        int row = startRow;
        createRow(sheet, row++, "№", "Тип токена", "Значение");

        List<Token> list = sorted
                ? tokens.stream()
                .sorted(Comparator.comparing(t -> t.getType().toString()))
                .collect(Collectors.toList())
                : tokens;

        int num = 1;
        for (Token token : list) {
            createRow(sheet, row++,
                    String.valueOf(num++),
                    token.getType().toString(),
                    token.getValue());
        }
    }


    private Map<TokenType, Integer> countByType(List<Token> tokens) {
        Map<TokenType, Integer> counter = new HashMap<>();
        tokens.forEach(t -> counter.merge(t.getType(), 1, Integer::sum));
        return counter;
    }

    //Фильтр, получить все типы кроме пробелов
    private TokenType[] relevantTypes() {
        return Arrays.stream(TokenType.values())
                .filter(t -> t != TokenType.WHITESPACE)
                .toArray(TokenType[]::new);
    }

    private void createRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void autoSizeAllSheets(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Row header = sheet.getRow(0);
            if (header != null) {
                for (int c = 0; c < header.getLastCellNum(); c++) {
                    sheet.autoSizeColumn(c);
                }
            }
        }
    }
}
