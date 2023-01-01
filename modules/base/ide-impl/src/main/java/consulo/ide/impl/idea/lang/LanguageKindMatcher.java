// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.lang;

import consulo.language.Language;

import javax.annotation.Nonnull;

final class LanguageKindMatcher extends LanguageMatcher {

  private final
  @Nonnull
  Language myLanguage;

  LanguageKindMatcher(@Nonnull Language language) {
    myLanguage = language;
  }

  @Override
  public boolean matchesLanguage(@Nonnull Language language) {
    return language.isKindOf(myLanguage);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LanguageKindMatcher matcher = (LanguageKindMatcher)o;

    if (!myLanguage.equals(matcher.myLanguage)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myLanguage.hashCode();
  }

  @Override
  public String toString() {
    return myLanguage + " with dialects";
  }
}
