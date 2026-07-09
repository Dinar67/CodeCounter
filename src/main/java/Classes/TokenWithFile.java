package Classes;

import LanguageLexer.LanguageToken.Token;

public class TokenWithFile {
    String fileName;
    Token token;

    TokenWithFile(String fileName, Token token) {
        this.fileName = fileName;
        this.token = token;
    }
}