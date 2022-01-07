package com.intellij.model.search.impl;

import com.intellij.lang.LanguageMatcher;
import javax.annotation.Nonnull;

// from kotlin
public abstract class LanguageInfo {
  private LanguageInfo() {
  }

  public static class NoLanguage extends LanguageInfo {
    public static NoLanguage INSTANCE = new NoLanguage();

    private NoLanguage() {
    }
  }

  public static class InLanguage extends LanguageInfo {
    private LanguageMatcher matcher;

    public InLanguage(@Nonnull LanguageMatcher matcher) {
      super();
      this.matcher = matcher;
    }

    @Nonnull
    public final LanguageMatcher getMatcher() {
      return matcher;
    }
  }
}

