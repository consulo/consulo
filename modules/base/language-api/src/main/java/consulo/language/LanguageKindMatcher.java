// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language;

import org.jspecify.annotations.Nullable;

final class LanguageKindMatcher extends LanguageMatcher {
    private final Language myLanguage;

    LanguageKindMatcher(Language language) {
        myLanguage = language;
    }

    @Override
    public boolean matchesLanguage(Language language) {
        return language.isKindOf(myLanguage);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LanguageKindMatcher that = (LanguageKindMatcher) o;

        return myLanguage.equals(that.myLanguage);
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
