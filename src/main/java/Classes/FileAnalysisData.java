package Classes;

import LanguageLexer.LanguageToken.TokenType;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class FileAnalysisData {
    public final File file;
    public int lineCount;
    public int nonEmptyLineCount;
    public final Map<TokenType, Integer> tokenTypeCount = new EnumMap<>(TokenType.class);
    public final Map<TokenType, Map<String, Integer>> tokenCount = new HashMap<>();

    public FileAnalysisData(File file) {
        this.file = file;
    }
}