package Classes;

import LanguageLexer.LanguageToken.TokenType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisResultData {
    private List<File> files = new ArrayList<>();
    private Map<TokenType, Integer> tokenTypeCount = new HashMap<>();
    private Map<TokenType, HashMap<String, Integer>> tokenCount = new HashMap<>();
    private long lineCount = 0;
    private long nonEmptyLineCount = 0;

    public AnalysisResultData(){}

    public AnalysisResultData(
            List<File> files,
            Map<TokenType, Integer> tokenTypeCount,
            Map<TokenType, HashMap<String, Integer>> tokenCount,
            int lineCount,
            int nonEmptyLineCount){
        this.files = files;
        this.tokenTypeCount = tokenTypeCount;
        this.lineCount = lineCount;
        this.nonEmptyLineCount = nonEmptyLineCount;
        this.tokenCount = tokenCount;
    }

    public List<File> getFiles() { return files; }
    public Map<TokenType, Integer> getTokenTypeCount() { return tokenTypeCount; }
    public Map<TokenType, HashMap<String, Integer>> getTokenCount() { return tokenCount; }
    public long getLineCount() { return lineCount; }
    public long getNonEmptyLineCount() { return nonEmptyLineCount; }

    public void setFiles(List<File> files) { this.files = files; }
    public void setTokenTypeCount(Map<TokenType, Integer> tokenTypeCount) { this.tokenTypeCount = tokenTypeCount; }
    public void setTokenCount(Map<TokenType, HashMap<String, Integer>> tokenCount) { this.tokenCount = tokenCount; }
    public void setLineCount(long lineCount) { this.lineCount = lineCount; }
    public void setNonEmptyLineCount(long nonEmptyLineCount) { this.nonEmptyLineCount = nonEmptyLineCount; }
}
