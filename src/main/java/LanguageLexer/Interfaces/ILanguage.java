package LanguageLexer.Interfaces;

import LanguageLexer.Lexer.RegexRule;

import java.util.List;

public interface ILanguage {
   public List<RegexRule> getRules();
   public List<ITokenPostProcessor> getPostProcessor();
}
