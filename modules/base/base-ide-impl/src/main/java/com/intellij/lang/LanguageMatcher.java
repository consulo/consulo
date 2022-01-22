// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang;

import consulo.language.Language;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

public abstract class LanguageMatcher {

  LanguageMatcher() {
  }

  public abstract boolean matchesLanguage(@Nonnull Language language);

  /**
   * Given the filter language X returns the matcher which matches language L if one the following is {@code true}:
   * <ul>
   * <li>X is not a metalanguage and X is L</li>
   * <li>X is a {@linkplain MetaLanguage#matchesLanguage metalanguage of L}</li>
   * </ul>
   */
  @Contract(pure = true)
  @Nonnull
  public static LanguageMatcher match(@Nonnull Language language) {
    if (language instanceof MetaLanguage) {
      return new MetaLanguageMatcher((MetaLanguage)language);
    }
    else {
      return new ExactMatcher(language);
    }
  }

  /**
   * Given the filter language X returns the matcher which matches language L if one the following is {@code true}:
   * <ul>
   * <li>X is not a metalanguage and X is a {@linkplain Language#getBaseLanguage base language of L}: {@code L.isKindOf(X) == true}</li>
   * <li>X is a {@linkplain MetaLanguage#matchesLanguage metalanguage of L} or one of its base languages</li>
   * </ul>
   */
  @Contract(pure = true)
  @Nonnull
  public static LanguageMatcher matchWithDialects(@Nonnull Language language) {
    if (language instanceof MetaLanguage) {
      return new MetaLanguageKindMatcher((MetaLanguage)language);
    }
    else {
      return new LanguageKindMatcher(language);
    }
  }
}

