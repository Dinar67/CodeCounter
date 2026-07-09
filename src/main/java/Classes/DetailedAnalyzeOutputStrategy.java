package Classes;

import LanguageLexer.LanguageToken.TokenType;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DetailedAnalyzeOutputStrategy extends CompactAnalyzeOutputStrategy {

    // Храним только готовый текст по файлу, а не токены - это на порядки меньше по памяти
    private final ConcurrentHashMap<File, String> fileBlocks = new ConcurrentHashMap<>();

    public DetailedAnalyzeOutputStrategy(AnalysisSettings settings) {
        super(settings);
    }

    @Override
    public void onFileAnalyzed(FileAnalysisData fileData) {
        StringBuilder block = new StringBuilder();
        block.append("----------------------------------------\n");
        block.append("ФАЙЛ: ").append(fileData.file.getName()).append("\n");
        block.append("----------------------------------------\n");
        block.append("Строк: ").append(fileData.lineCount)
                .append(" (непустых: ").append(fileData.nonEmptyLineCount).append(")\n");

        appendFileCounts(block, fileData.tokenTypeCount, fileData.tokenCount);
        block.append("\n");

        fileBlocks.put(fileData.file, block.toString());
    }

    @Override
    public String finalizeOutput(AnalysisResultData aggregate) {
        StringBuilder sb = new StringBuilder(super.finalizeOutput(aggregate));

        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("=== АНАЛИЗ ПО ФАЙЛАМ ===\n\n");

        // Восстанавливаем исходный порядок файлов, даже если анализ шёл параллельно
        for (File file : aggregate.getFiles()) {
            String block = fileBlocks.get(file);
            if (block != null) sb.append(block);
        }

        return sb.toString();
    }

    // Локальная версия appendCounts для не-потокобезопасных Map<TokenType, Integer>
    private void appendFileCounts(StringBuilder sb,
                                  Map<TokenType, Integer> tokenTypeCount,
                                  Map<TokenType, Map<String, Integer>> tokenCount) {

        int totalTokens = tokenTypeCount.values().stream().mapToInt(Integer::intValue).sum();
        sb.append("Токенов всего: ").append(totalTokens).append("\n\n");

        sb.append("По типам:\n");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            int count = tokenTypeCount.getOrDefault(type, 0);
            if (count > 0) sb.append(String.format("  %-14s %d\n", type, count));
        }

        if (!tokenCount.isEmpty()) {
            sb.append("\n");
            for (String typeName : new String[]{"KEYWORD", "IDENTIFIER", "LITERAL", "OPERATOR", "SEPARATOR", "COMMENT"}) {
                TokenType type;
                try { type = TokenType.valueOf(typeName); }
                catch (IllegalArgumentException e) { continue; }
                if (!tokenCount.containsKey(type)) continue;

                Map<String, Integer> values = tokenCount.get(type);
                int typeTotal = values.values().stream().mapToInt(Integer::intValue).sum();
                sb.append(type).append(" (").append(typeTotal).append(" шт.):\n");
                values.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(e -> sb.append(String.format("  %-25s %d\n", e.getKey(), e.getValue())));
                sb.append("\n");
            }
        }
    }
}