package Classes;

import Classes.AnalysisSettings;
import Interfaces.IAnalyzeOutputStrategy;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CompactAnalyzeOutputStrategy implements IAnalyzeOutputStrategy {

    protected final AnalysisSettings settings;

    // Порядок типов в выводе
    private static final String[] TYPE_ORDER =
            {"KEYWORD", "IDENTIFIER", "LITERAL", "OPERATOR", "SEPARATOR", "COMMENT"};

    public CompactAnalyzeOutputStrategy(AnalysisSettings settings) {
        this.settings = settings;
    }

    @Override
    public String makeAnalyzeOutput(HashMap<File, List<Token>> filesTokensMap) {
        StringBuilder sb = new StringBuilder();

        // Собираем общую статистику
        Map<TokenType, Integer> totalCounter = new HashMap<>();
        Map<TokenType, Map<String, Integer>> totalLexemeGroups = new HashMap<>();
        int totalLines = 0;
        int totalNonEmptyLines = 0;

        for (Map.Entry<File, List<Token>> entry : filesTokensMap.entrySet()) {
            String code = ""; // строки считаем отдельно если нужно
            for (Token token : entry.getValue()) {
                totalCounter.merge(token.getType(), 1, Integer::sum);
                if (shouldShow(token)) {
                    totalLexemeGroups
                            .computeIfAbsent(token.getType(), k -> new HashMap<>())
                            .merge(token.getValue(), 1, Integer::sum);
                }
            }
        }

        int totalTokens = totalCounter.values().stream().mapToInt(Integer::intValue).sum();

        // Общая шапка
        sb.append("=== ОБЩАЯ СТАТИСТИКА ===\n");
        sb.append("Файлов: ").append(filesTokensMap.size()).append("\n");
        sb.append("Токенов всего: ").append(totalTokens).append("\n\n");

        // По типам
        sb.append("По типам:\n");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            int count = totalCounter.getOrDefault(type, 0);
            if (count > 0) {
                sb.append(String.format("  %-14s %d\n", type, count));
            }
        }

        // Список лексем
        if (!totalLexemeGroups.isEmpty()) {
            sb.append("\n");
            appendLexemeList(sb, totalLexemeGroups);
        }

        return sb.toString();
    }

    // Переиспользуется в DetailedAnalyzeOutputStrategy для блока каждого файла
    protected void appendFileBlock(StringBuilder sb, File file,
                                   List<Token> tokens, int lines, int nonEmptyLines) {
        Map<TokenType, Integer> fileCounter = new HashMap<>();
        Map<TokenType, Map<String, Integer>> fileLexemeGroups = new HashMap<>();

        for (Token token : tokens) {
            fileCounter.merge(token.getType(), 1, Integer::sum);
            if (shouldShow(token)) {
                fileLexemeGroups
                        .computeIfAbsent(token.getType(), k -> new HashMap<>())
                        .merge(token.getValue(), 1, Integer::sum);
            }
        }

        sb.append("========================================\n");
        sb.append("ФАЙЛ: ").append(file.getName()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Строк: ").append(lines)
                .append(" (непустых: ").append(nonEmptyLines).append(")\n");
        sb.append("Всего токенов: ").append(tokens.size()).append("\n\n");

        sb.append("По типам:\n");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            int count = fileCounter.getOrDefault(type, 0);
            if (count > 0) {
                sb.append(String.format("  %-14s %d\n", type, count));
            }
        }

        if (!fileLexemeGroups.isEmpty()) {
            sb.append("\n");
            appendLexemeList(sb, fileLexemeGroups);
        }
        sb.append("\n");
    }

    protected void appendLexemeList(StringBuilder sb,
                                    Map<TokenType, Map<String, Integer>> groups) {
        for (String typeName : TYPE_ORDER) {
            TokenType type;
            try { type = TokenType.valueOf(typeName); }
            catch (IllegalArgumentException e) { continue; }

            if (!groups.containsKey(type)) continue;

            Map<String, Integer> values = groups.get(type);
            int typeTotal = values.values().stream().mapToInt(Integer::intValue).sum();

            sb.append(type).append(" (").append(typeTotal).append(" шт.):\n");
            values.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> sb.append(String.format("  %-25s %d\n",
                            e.getKey(), e.getValue())));
            sb.append("\n");
        }
    }

    protected boolean shouldShow(Token token) {
        if (token.getType() == TokenType.WHITESPACE) return false;
        return settings.selectedTokenTypes.contains(token.getType());
    }
}