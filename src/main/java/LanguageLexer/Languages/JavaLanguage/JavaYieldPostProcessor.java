package LanguageLexer.Languages.JavaLanguage;

import LanguageLexer.Interfaces.ITokenPostProcessor;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;

import java.util.ArrayList;
import java.util.List;

public class JavaYieldPostProcessor implements ITokenPostProcessor {
    @Override
    public List<Token> process(List<Token> tokens) {
        List<Token> newTokens = new ArrayList<>();

        for(int i = 0; i < tokens.size(); i++){
            var token = tokens.get(i);
            if(token.getValue().toLowerCase().matches("yield") && i != tokens.size()-1){
                var nextToken = tokens.get(i+1);

                if(nextToken.getType() == TokenType.LITERAL ||
                   nextToken.getType() == TokenType.IDENTIFIER ||
                   nextToken.getValue() == "(" ||
                   nextToken.getValue() == "!" ||
                   nextToken.getValue() == "++" ||
                   nextToken.getValue() == "--")
                    token = new Token(TokenType.KEYWORD, "yield", token.getStart(), token.getEnd());
                else token = new Token(TokenType.IDENTIFIER, "yield", token.getStart(), token.getEnd());
            }
            newTokens.add(token);
        }

        return newTokens;
    }
}
