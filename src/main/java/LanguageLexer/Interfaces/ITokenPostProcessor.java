package LanguageLexer.Interfaces;

import LanguageLexer.LanguageToken.Token;

import java.util.List;

public interface ITokenPostProcessor {
    public List<Token> process(List<Token> tokens);
}
