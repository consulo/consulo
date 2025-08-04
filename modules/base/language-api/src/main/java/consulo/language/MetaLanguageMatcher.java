// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language;

import jakarta.annotation.Nonnull;

final class MetaLanguageMatcher extends LanguageMatcher {

  private final
  @Nonnull
  MetaLanguage myLanguage;

  MetaLanguageMatcher(@Nonnull MetaLanguage language) {
    myLanguage = language;
  }

  @Override
  public boolean matchesLanguage(@Nonnull Language language) {
    return myLanguage.matchesLanguage(language);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetaLanguageMatcher matcher = (MetaLanguageMatcher)o;

    if (!myLanguage.equals(matcher.myLanguage)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myLanguage.hashCode();
  }

  @Override
  public String toString() {
    return myLanguage + " (meta)";
  }
}
