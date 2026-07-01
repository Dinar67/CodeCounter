package LanguageLexer.LanguageToken;

public class Token {
    private TokenType _type;
    private String _value;
    private int _start;
    private int _end;

    public Token(TokenType type, String value, int start, int end){
        _type = type;
        _start = start;
        _end = end;
        _value = value;
    }

    public TokenType getType() { return _type; }
    public String getValue() { return _value; }
    public int getStart() { return _start; }
    public int getEnd() { return _end; }
}
