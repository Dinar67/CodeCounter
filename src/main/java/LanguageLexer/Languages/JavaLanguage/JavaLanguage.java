package LanguageLexer.Languages.JavaLanguage;

import LanguageLexer.Interfaces.ILanguage;
import LanguageLexer.Interfaces.ITokenPostProcessor;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Lexer.RegexRule;

import java.util.List;

public class JavaLanguage implements ILanguage {

    @Override
    public List<RegexRule> getRules() {
        return List.of(
                // 0. Пробелы (игнорируются)
                new RegexRule(TokenType.WHITESPACE, "\\s+", 0),

                // 1. Комментарии (однострочные, многострочные, javadoc)
                new RegexRule(TokenType.COMMENT, "(//[^\n]*)|(/\\*[\\s\\S]*?\\*/)", 10),

                // 2. Литералы (Порядок важен: от более специфичных к общим)
                new RegexRule(TokenType.LITERAL,
                // 2.1 Текстовые блоки
                "\"\"\"[\\s\\S]*?\"\"\"|" +
                // 2.2 Строковые литералы "..."
                "\"(?:\\\\.|[^\"\\\\])*\"|" +
                // 2.3 Символьные литералы '...' (включая \n, \t, \', \\, uXXXX)
                "'(?:\\\\[btnfr\"'\\\\]|\\\\u[0-9a-fA-F]{4}|[^'\\\\])'|" +
                // 2.4 Числа: Шестнадцатеричные (0x...) и Двоичные (0b...) с поддержкой _
                "0[xX][0-9a-fA-F_]+[lL]?|" +
                "0[bB][01_]+[lL]?|" +
                // 2.5 Числа: Восьмеричные (начинаются с 0, но не 0x/0b)
                "0[0-7_]+[lL]?|" +
                // 2.6 Числа: С плавающей точкой (включая .5f, 1e10, 1_000.5)
                "\\d[\\d_]*\\.\\d[\\d_]*([eE][+-]?\\d[\\d_]*)?[fFdD]?|" +
                "\\.\\d[\\d_]*([eE][+-]?\\d[\\d_]*)?[fFdD]?|" +
                "\\d[\\d_]*[eE][+-]?\\d[\\d_]*[fFdD]?|" +
                // 2.7 Числа: Целые (с поддержкой 1_000_000L)
                "\\d[\\d_]*[lL]?|" +
                // 2.8 Булевы и null
                "\\b(?:true|false|null)\\b",20),

                // 3. Ключевые слова
                new RegexRule(TokenType.KEYWORD,
                    "\\b(?:" +
                    // Базовые
                    "abstract|assert|boolean|break|byte|case|catch|char|class|continue|" +
                    "default|do|double|else|enum|extends|final|finally|float|for|" +
                    "if|implements|import|instanceof|int|interface|long|native|new|" +
                    "package|private|protected|public|return|short|static|strictfp|" +
                    "super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|" +
                    // Современные (Java 10+)
                    "var|yield|record|sealed|permits|when|" +
                    // Псевдо-ключевое (часто подсвечивается как ключевое)
                    "String" +
                    ")\\b", 30),

                // 4. Операторы
                new RegexRule(TokenType.OPERATOR,
                        // 3-символьные
                        "<<=|>>=|>>>|&&=|\\|\\|=|" +
                        // 2-символьные
                        "==|!=|>=|<=|&&|\\|\\||\\+=|-=|\\*=|/=|%=|\\+\\+|--|->|::|" +
                        // 1-символьные
                        "\\+|-|\\*|/|%|>|<|!|=|\\?|:|&|\\||\\^|~|@",40),

                // 5. Разделители
                new RegexRule(TokenType.SEPARATOR, "\\{|}|\\[|]|\\(|\\)|;|\\.|,|\\.\\.\\.", 50),

                // 6. Идентификаторы
                new RegexRule(TokenType.IDENTIFIER, "\\p{L}_\\p{N}]*", 60)
        );
    }

    @Override
    public List<ITokenPostProcessor> getPostProcessor() {
        return List.of(
                new JavaYieldPostProcessor()
        );
    }
}