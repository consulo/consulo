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
package consulo.diff.impl.internal.util;

import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public enum HighlightPolicy {
    DO_NOT_HIGHLIGHT(DiffLocalize.optionHighlightingPolicyNone()),
    BY_LINE(DiffLocalize.optionHighlightingPolicyLines()),
    BY_WORD(DiffLocalize.optionHighlightingPolicyWords()),
    BY_WORD_SPLIT(DiffLocalize.optionHighlightingPolicySplit());

    @Nonnull
    private final LocalizeValue myText;

    HighlightPolicy(@Nonnull LocalizeValue text) {
        myText = text;
    }

    @Nonnull
    public LocalizeValue getText() {
        return myText;
    }

    public boolean isShouldCompare() {
        return this != DO_NOT_HIGHLIGHT;
    }

    public boolean isFineFragments() {
        return this == BY_WORD || this == BY_WORD_SPLIT;
    }

    public boolean isShouldSquash() {
        return this == BY_WORD || this == BY_LINE;
    }
}
