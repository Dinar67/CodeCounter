package LanguageLexer.Lexer;

import LanguageLexer.LanguageToken.TokenType;

public class RegexRule {
    private TokenType _type;
    private String _rule;
    private int _priority;

    public RegexRule(TokenType type, String rule, int priority){
        _type = type;
        _rule = rule;
        _priority = priority;
    }

    public TokenType getType() { return _type; }
    public String getRule() { return _rule; }
    public int getPriority() { return _priority; }
}
