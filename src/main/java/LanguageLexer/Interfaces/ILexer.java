package LanguageLexer.Interfaces;

import LanguageLexer.LanguageToken.Token;

import java.util.List;

public interface ILexer {
    public List<Token> tokenize(String code);
}
