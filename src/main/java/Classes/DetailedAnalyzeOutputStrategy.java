package Classes;

import LanguageLexer.LanguageToken.Token;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class DetailedAnalyzeOutputStrategy extends CompactAnalyzeOutputStrategy {

    public DetailedAnalyzeOutputStrategy(AnalysisSettings settings) {
        super(settings);
    }

    @Override
    public String makeAnalyzeOutput(HashMap<File, List<Token>> filesTokensMap) {
        // Общая часть — из родителя
        StringBuilder sb = new StringBuilder(super.makeAnalyzeOutput(filesTokensMap));

        // Добавляем блок по файлам
        sb.append("=".repeat(60)).append("\n");
        sb.append("=== АНАЛИЗ ПО ФАЙЛАМ ===\n\n");

        for (Map.Entry<File, List<Token>> entry : filesTokensMap.entrySet()) {
            File file = entry.getKey();
            List<Token> tokens = entry.getValue();

            int lines = 0;
            int nonEmptyLines = 0;
            try {
                String code = Files.readString(file.toPath());
                lines = code.split("\n").length;
                nonEmptyLines = (int) Arrays.stream(code.split("\n"))
                        .filter(l -> !l.trim().isEmpty()).count();
            } catch (Exception e) {
                sb.append("ОШИБКА чтения ").append(file.getName()).append("\n\n");
                continue;
            }

            appendFileBlock(sb, file, tokens, lines, nonEmptyLines);
        }

        return sb.toString();
    }
}