// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language;

import consulo.language.util.LanguageUtil;

import jakarta.annotation.Nonnull;

final class MetaLanguageKindMatcher extends LanguageMatcher {

  @Nonnull
  private final MetaLanguage myLanguage;

  MetaLanguageKindMatcher(@Nonnull MetaLanguage language) {
    myLanguage = language;
  }

  @Override
  public boolean matchesLanguage(@Nonnull Language language) {
    return LanguageUtil.hierarchy(language).filter(myLanguage::matchesLanguage).isNotEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetaLanguageKindMatcher matcher = (MetaLanguageKindMatcher)o;

    if (!myLanguage.equals(matcher.myLanguage)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myLanguage.hashCode();
  }

  @Override
  public String toString() {
    return myLanguage + " (meta) with dialects";
  }
}
