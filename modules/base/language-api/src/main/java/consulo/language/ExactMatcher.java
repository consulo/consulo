/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language;

import org.jspecify.annotations.Nullable;

final class ExactMatcher extends LanguageMatcher {
    private final Language myLanguage;

    ExactMatcher(Language language) {
        myLanguage = language;
    }

    @Override
    public boolean matchesLanguage(Language language) {
        return myLanguage.is(language);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExactMatcher that = (ExactMatcher) o;

        return myLanguage.equals(that.myLanguage);
    }

    @Override
    public int hashCode() {
        return myLanguage.hashCode();
    }

    @Override
    public String toString() {
        return myLanguage.toString();
    }
}
