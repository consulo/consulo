package com.intellij.lang;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 18:05/30.05.13
 */
public interface LanguageVersionResolver {
  LanguageVersionResolver EMPTY = new LanguageVersionResolver() {

    @NotNull
    @Override
    public LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable PsiElement element) {
      final LanguageVersion[] versions = language.getVersions();
      if(versions.length == 0) {
        throw new IllegalArgumentException("Zero version count for language: " + language);
      }
      return versions[0];
    }
  };
  @NotNull
  LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable PsiElement element);
}
