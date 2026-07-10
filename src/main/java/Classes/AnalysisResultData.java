package Classes;

import Interfaces.ICodeSource;
import LanguageLexer.LanguageToken.TokenType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class AnalysisResultData {

    private List<ICodeSource> sources;
    private final LongAdder lineCount = new LongAdder();
    private final LongAdder nonEmptyLineCount = new LongAdder();

    private final ConcurrentHashMap<TokenType, LongAdder> tokenTypeCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TokenType, ConcurrentHashMap<String, LongAdder>> tokenCount = new ConcurrentHashMap<>();

    public List<ICodeSource> getSources() { return sources; }
    public void setSources(List<ICodeSource> sources) { this.sources = sources; }

    public void addLines(int lines, int nonEmpty) {
        lineCount.add(lines);
        nonEmptyLineCount.add(nonEmpty);
    }

    public long getLineCount() { return lineCount.sum(); }
    public long getNonEmptyLineCount() { return nonEmptyLineCount.sum(); }

    public void addTokenType(TokenType type) {
        tokenTypeCount.computeIfAbsent(type, t -> new LongAdder()).increment();
    }

    public void addLexeme(TokenType type, String value) {
        tokenCount.computeIfAbsent(type, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(value, v -> new LongAdder())
                .increment();
    }

    public Map<TokenType, LongAdder> getTokenTypeCount() { return tokenTypeCount; }
    public Map<TokenType, ConcurrentHashMap<String, LongAdder>> getTokenCount() { return tokenCount; }
}