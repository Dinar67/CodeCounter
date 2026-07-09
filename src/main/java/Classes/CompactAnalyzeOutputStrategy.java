package Classes;

import Interfaces.IAnalyzeOutputStrategy;
import LanguageLexer.LanguageToken.TokenType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class CompactAnalyzeOutputStrategy implements IAnalyzeOutputStrategy {

    protected final AnalysisSettings settings;

    private static final String[] TYPE_ORDER =
            {"KEYWORD", "IDENTIFIER", "LITERAL", "OPERATOR", "SEPARATOR", "COMMENT"};

    public CompactAnalyzeOutputStrategy(AnalysisSettings settings) {
        this.settings = settings;
    }

    @Override
    public String finalizeOutput(AnalysisResultData aggregate) {
        StringBuilder sb = new StringBuilder();

        sb.append("Файлов: ").append(aggregate.getFiles().size()).append("\n");
        sb.append("Строк: ").append(aggregate.getLineCount())
                .append(" (непустых: ").append(aggregate.getNonEmptyLineCount()).append(")\n");

        appendCountsThread(sb, aggregate.getTokenTypeCount(), aggregate.getTokenCount());

        return sb.toString();
    }

    // ==== Версия для параллельного агрегата (весь проект, много потоков) ====

    public void appendCountsThread(StringBuilder sb,
                             Map<TokenType, LongAdder> tokenTypeCount,
                             Map<TokenType, ConcurrentHashMap<String, LongAdder>> tokenCount) {

        long totalTokens = tokenTypeCount.values().stream().mapToLong(LongAdder::sum).sum();
        sb.append("Токенов всего: ").append(totalTokens).append("\n\n");

        sb.append("По типам:\n");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            LongAdder counter = tokenTypeCount.get(type);
            long count = counter == null ? 0 : counter.sum();
            if (count > 0) {
                sb.append(String.format("  %-14s %d\n", type, count));
            }
        }

        if (!tokenCount.isEmpty()) {
            sb.append("\n");
            appendLexemeListThread(sb, tokenCount);
        }
    }

    protected void appendLexemeListThread(StringBuilder sb, Map<TokenType, ConcurrentHashMap<String, LongAdder>> groups) {
        for (String typeName : TYPE_ORDER) {
            TokenType type;
            try { type = TokenType.valueOf(typeName); }
            catch (IllegalArgumentException e) { continue; }

            if (!groups.containsKey(type)) continue;

            var values = groups.get(type);
            long typeTotal = values.values().stream().mapToLong(LongAdder::sum).sum();

            sb.append(type).append(" (").append(typeTotal).append(" шт.):\n");
            values.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                    .forEach(e -> sb.append(String.format("  %-25s %d\n", e.getKey(), e.getValue().sum())));
            sb.append("\n");
        }
    }

    // ==== Версия для одного файла (синхронно, один поток, обычные HashMap) ====

    public void appendCounts(StringBuilder sb,
                             Map<TokenType, Integer> tokenTypeCount,
                             Map<TokenType, HashMap<String, Integer>> tokenCount) {

        long totalTokens = tokenTypeCount.values().stream().mapToLong(Integer::longValue).sum();
        sb.append("Токенов всего: ").append(totalTokens).append("\n\n");

        sb.append("По типам:\n");
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            int count = tokenTypeCount.getOrDefault(type, 0);
            if (count > 0) {
                sb.append(String.format("  %-14s %d\n", type, count));
            }
        }

        if (!tokenCount.isEmpty()) {
            sb.append("\n");
            appendLexemeList(sb, tokenCount);
        }
    }

    protected void appendLexemeList(StringBuilder sb, Map<TokenType, HashMap<String, Integer>> groups) {
        for (String typeName : TYPE_ORDER) {
            TokenType type;
            try { type = TokenType.valueOf(typeName); }
            catch (IllegalArgumentException e) { continue; }

            if (!groups.containsKey(type)) continue;

            var values = groups.get(type);
            int typeTotal = values.values().stream().mapToInt(Integer::intValue).sum();

            sb.append(type).append(" (").append(typeTotal).append(" шт.):\n");
            values.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> sb.append(String.format("  %-25s %d\n", e.getKey(), e.getValue())));
            sb.append("\n");
        }
    }
}