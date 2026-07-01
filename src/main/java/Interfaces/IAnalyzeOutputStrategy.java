package Interfaces;

import LanguageLexer.LanguageToken.Token;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public interface IAnalyzeOutputStrategy {
    public String makeAnalyzeOutput(HashMap<File, List<Token>> filesTokensMap);
}
