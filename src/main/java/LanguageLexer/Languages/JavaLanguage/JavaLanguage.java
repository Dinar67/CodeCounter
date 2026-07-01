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
                new RegexRule(TokenType.WHITESPACE, "\\s+", 0),
                new RegexRule(TokenType.SEPARATOR, "\\{|}|\\[|]|\\(|\\)|;|\\.|,", 2),
                new RegexRule(TokenType.COMMENT, "(//[^\n]*)|(/\\*[\\s\\S]*?\\*/)|(/\\*\\*[\\s\\S]*?\\*/)", 3),
                new RegexRule(TokenType.LITERAL,
                        // 1. Строковые литералы "..."
                        "\"(?:\\\\.|[^\"\\\\])*\"|" +
                        // 2. Символьные литералы '...'
                        "'(?:\\\\.|[^'\\\\])'|" +
                        // 3. Числа с плавающей точкой и научная запись
                        "\\d+\\.\\d+[eE][+-]?\\d+[fFdD]?|" +    // 3.14e10, 1.5e-3f
                        "\\d+\\.\\d+[fFdD]?|" +                 // 3.14, 2.5f
                        "\\d+[eE][+-]?\\d+[fFdD]?|" +           // 1e10, 2E+5
                        "\\d+[fFdD]|" +                         // 2.5f (без точки)

                        // 4. Целые числа
                        "\\d+[lL]?|" +                          // 42, 100L
                        // 5. Булевы и null
                        "\\b(?:true|false|null)\\b",
                        4),
                new RegexRule(TokenType.KEYWORD, "\\b(?:if|else|while|int|double|boolean|char|void|byte|float|long|String|for|do|switch|case|break|return|continue|public|private|static|final|class|interface|extends|implements|new|this|super|try|catch|finally|throw|throws|package|import|yield)\\b", 5),
                new RegexRule(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*", 6),
                new RegexRule(TokenType.OPERATOR,
                        // 3-символьные операторы
                        "<<=|>>=|>>>|&&=|\\|\\|=|" +
                        // 2-символьные операторы
                        "==|!=|>=|<=|&&|\\|\\||\\+=|-=|\\*=|/=|\\+\\+|--|->|::|" +
                        // 1-символьные операторы
                        "\\+|-|\\*|/|%|>|<|!|=|\\?|:|&|\\|", 7)
        );
    }

    @Override
    public List<ITokenPostProcessor> getPostProcessor() {
        return List.of(new JavaYieldPostProcessor());
    }
}
