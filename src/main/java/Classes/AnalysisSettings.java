package Classes;

import LanguageLexer.LanguageToken.TokenType;
import java.util.HashSet;
import java.util.Set;

public class AnalysisSettings {
    public boolean showFileDetails = true;
    public Set<TokenType> selectedTokenTypes = new HashSet<>();

    public AnalysisSettings() {}
    public AnalysisSettings(boolean showFileDetails, Set<TokenType> selectedTokenTypes) {
        this.showFileDetails = showFileDetails;
        this.selectedTokenTypes = selectedTokenTypes;
    }
}