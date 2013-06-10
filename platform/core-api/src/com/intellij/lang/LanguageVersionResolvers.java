package com.intellij.lang;

/**
 * @author VISTALL
 * @since 18:04/30.05.13
 */
public class LanguageVersionResolvers extends LanguageExtension<LanguageVersionResolver> {
  public static final LanguageVersionResolvers INSTANCE = new LanguageVersionResolvers();

  private LanguageVersionResolvers() {
    super("com.intellij.lang.versionResolver", LanguageVersionResolver.EMPTY);
  }
}
