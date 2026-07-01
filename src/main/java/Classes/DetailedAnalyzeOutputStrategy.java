package Classes;

import Interfaces.IAnalyzeOutputStrategy;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailedAnalyzeOutputStrategy implements IAnalyzeOutputStrategy {

    @Override
    public String makeAnalyzeOutput(HashMap<File, List<Token>> filesTokensMap) {
        StringBuilder fullResult = new StringBuilder();
        Map<TokenType, Integer> totalCounter = new HashMap<>();
        HashMap<String, Integer> generalKeywordsCount = new HashMap<>();

        fullResult.append("\n\n\n=== АНАЛИЗ ВЫБРАННЫХ ФАЙЛОВ ===\n");
        fullResult.append("Всего файлов: ").append(filesTokensMap.size()).append("\n\n\n");

        for (Map.Entry<File, List<Token>> entry : filesTokensMap.entrySet()) {
            File file = entry.getKey();
            List<Token> tokens = entry.getValue();

            Map<TokenType, Integer> fileCounter = new HashMap<>();
            tokens.forEach(token -> {
                fileCounter.merge(token.getType(), 1, Integer::sum);
                totalCounter.merge(token.getType(), 1, Integer::sum);
            });

            fullResult.append("========================================\n");
            fullResult.append("ФАЙЛ: ").append(file.getName()).append("\n");
            fullResult.append("----------------------------------------\n");
            fullResult.append("Всего токенов: ").append(tokens.size()).append("\n");
            fullResult.append("Статистика:\n");
            for (TokenType type : TokenType.values()) {
                if (type != TokenType.WHITESPACE) {
                    int count = fileCounter.getOrDefault(type, 0);
                    if (count > 0) {
                        fullResult.append("  ").append(type).append(": ").append(count).append("\n");
                    }
                }
            }

            fullResult.append("----------------------------------------------\n");
            fullResult.append("КЛЮЧЕВЫЕ СЛОВА И КОЛ-ВО ИСПОЛЬЗОВАНИЙ В ФАЙЛЕ:\n");
            fullResult.append("----------------------------------------------\n");

            HashMap<String, Integer> keywordsCount = new HashMap<>();
            for (Token token : tokens) {
                if (token.getType() == TokenType.KEYWORD) {
                    String value = token.getValue().toLowerCase();
                    keywordsCount.merge(value, 1, Integer::sum);
                    generalKeywordsCount.merge(value, 1, Integer::sum);
                }
            }
            keywordsCount.forEach((k, v) ->
                    fullResult.append("\t").append(k).append(": ").append(v).append("\n"));

            fullResult.append("========================================\n\n\n");
        }

        // Общая статистика — вставляем в начало
        int totalTokens = totalCounter.values().stream().mapToInt(Integer::intValue).sum();

        StringBuilder header = new StringBuilder();
        header.append("=== ОБЩАЯ СТАТИСТИКА ===\n");
        header.append("Всего токенов: ").append(totalTokens).append("\n");
        for (TokenType type : TokenType.values()) {
            if (type != TokenType.WHITESPACE) {
                int count = totalCounter.getOrDefault(type, 0);
                if (count > 0) {
                    header.append("\t").append(type).append(": ").append(count).append("\n");
                }
            }
        }
        header.append("Ключевые слова и кол-во использований:\n");
        generalKeywordsCount.forEach((k, v) ->
                header.append("\t").append(k).append(": ").append(v).append("\n"));
        header.append("\n\n");

        return header.append(fullResult).toString();
    }
}
