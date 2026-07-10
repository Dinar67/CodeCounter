package com.example.codecounter;

import Classes.AnalysisSettings;
import Classes.CompactAnalyzeOutputStrategy;
import Interfaces.ICodeSource;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import Services.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class FileAnalysisStageController {

    @FXML protected TextArea analysisTextArea;
    @FXML protected TextArea codeTextArea;

    public void transferData(ICodeSource codeSource) {
        try {
            analyze(codeSource);
        } catch (Exception e) {
            analysisTextArea.setText("Ошибка анализа: " + e.getMessage());
            codeTextArea.setText("");
        }
    }

    private void analyze(ICodeSource codeSource) throws Exception {
        String code = codeSource.readText();
        codeTextArea.setText(code);

        var settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
        if (settingsService == null) throw new Exception("Сервис настроек отсутствует!");
        AnalysisSettings settings = settingsService.getSettings(1);
        Set<TokenType> selectedTypes = Set.copyOf(settings.selectedTokenTypes);

        var lexer = new RegexLexer(new JavaLanguage());
        List<Token> tokens = lexer.tokenize(code);

        HashMap<TokenType, Integer> tokenTypeCount = new HashMap<>();
        HashMap<TokenType, HashMap<String, Integer>> tokenCount = new HashMap<>();

        for (Token token : tokens) {
            if(settings.mergeBracketВelimiters && checkCloseBracket(token)) continue;
            tokenTypeCount.merge(token.getType(), 1, Integer::sum);

            if (selectedTypes.contains(token.getType())) {
                tokenCount.computeIfAbsent(token.getType(), k -> new HashMap<>())
                        .merge(mergeBracketDelimetrs(token, settings), 1, Integer::sum);
            }
        }

        int lines = (int) code.lines().count();
        int nonEmptyLines = (int) code.lines().filter(l -> !l.strip().isEmpty()).count();

        StringBuilder sb = new StringBuilder();
        sb.append("Файл: ").append(codeSource.getDisplayName()).append("\n");
        sb.append("Строк: ").append(lines).append(" (непустых: ").append(nonEmptyLines).append(")\n");

        // Переиспользуем форматирование из компактной стратегии - логика вывода одна и та же
        var strategy = new CompactAnalyzeOutputStrategy(settings);
        strategy.appendCounts(sb, tokenTypeCount, tokenCount);

        analysisTextArea.setText(sb.toString());
    }

    private boolean checkCloseBracket(Token token) {
        return token.getValue().equals(")") || token.getValue().equals("]") || token.getValue().equals("}");
    }

    private String mergeBracketDelimetrs(Token token, AnalysisSettings settings){
        HashMap<String, String> brackets = new HashMap<>() {{
            put("(", ")");
            put("[", "]");
            put("{", "}");
        }};

        if(settings.mergeBracketВelimiters && brackets.containsKey(token.getValue())){
            var part = brackets.get(token.getValue());
            return token.getValue() + part;
        }
        return token.getValue();
    }
}