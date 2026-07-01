package LanguageLexer.Lexer;

import LanguageLexer.Interfaces.ILanguage;
import LanguageLexer.Interfaces.ILexer;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class RegexLexer implements ILexer {
    private Pattern _pattern;
    private List<TokenType> _usedTypes = new ArrayList<TokenType>();
    private ILanguage _language;

    public RegexLexer(ILanguage language){
        _language = language;
        var rules = new ArrayList<>(language.getRules());
        rules.sort(Comparator.comparing(RegexRule::getPriority));

        String regex = "";
        for (var rule : rules) {
            if (rule.getPriority() > 0) {
                regex += "(?<" + rule.getType().toString() + ">" + rule.getRule() + ")|";
                if (!_usedTypes.contains(rule.getType())) _usedTypes.add(rule.getType());
            }
        }

        _pattern = Pattern.compile(regex);
    }

    @Override
    public List<Token> tokenize(String code) {
        var matcher = _pattern.matcher(code);
        List<Token> list = new ArrayList<Token>();

        while (matcher.find()) {
            for(var type : _usedTypes){
                if(matcher.group(type.toString()) != null)
                    list.add(new Token(type, matcher.group(), matcher.start(), matcher.end()));
            }
        }

        for (var processor : _language.getPostProcessor())
            list = processor.process(list);

        return list;
    }
}
