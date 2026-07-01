package Classes;

import Interfaces.IAnalyzeOutputStrategy;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompactAnalyzeOutputStrategy implements IAnalyzeOutputStrategy {

    @Override
    public String makeAnalyzeOutput(HashMap<File, List<Token>> filesTokensMap) {
        StringBuilder result = new StringBuilder();
        result.append("=== КРАТКАЯ СТАТИСТИКА ===\n");
        result.append("Файлов проанализировано: ").append(filesTokensMap.size()).append("\n\n");

        Map<TokenType, Integer> totalCounter = new HashMap<>();
        filesTokensMap.values().forEach(tokens ->
                tokens.forEach(t -> totalCounter.merge(t.getType(), 1, Integer::sum)));

        int total = totalCounter.values().stream().mapToInt(Integer::intValue).sum();
        result.append("Всего токенов: ").append(total).append("\n");
        for (TokenType type : TokenType.values()) {
            if (type != TokenType.WHITESPACE) {
                int count = totalCounter.getOrDefault(type, 0);
                if (count > 0) {
                    result.append("  ").append(type).append(": ").append(count).append("\n");
                }
            }
        }
        return result.toString();
    }
}
