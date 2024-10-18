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

import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public enum IgnorePolicy {
    DEFAULT(ComparisonPolicy.DEFAULT, DiffLocalize.optionIgnorePolicyNone()),
    TRIM_WHITESPACES(ComparisonPolicy.TRIM_WHITESPACES, DiffLocalize.optionIgnorePolicyTrim()),
    IGNORE_WHITESPACES(ComparisonPolicy.IGNORE_WHITESPACES, DiffLocalize.optionIgnorePolicyWhitespaces()),
    IGNORE_WHITESPACES_CHUNKS(ComparisonPolicy.IGNORE_WHITESPACES, DiffLocalize.optionIgnorePolicyWhitespacesEmptyLines());

    @Nonnull
    private final ComparisonPolicy myComparisonPolicy;
    @Nonnull
    private final LocalizeValue myText;

    IgnorePolicy(@Nonnull ComparisonPolicy comparisonPolicy, @Nonnull LocalizeValue text) {
        myComparisonPolicy = comparisonPolicy;
        myText = text;
    }

    @Nonnull
    public LocalizeValue getText() {
        return myText;
    }

    @Nonnull
    public ComparisonPolicy getComparisonPolicy() {
        return myComparisonPolicy;
    }

    public boolean isShouldTrimChunks() {
        return this == IGNORE_WHITESPACES_CHUNKS;
    }
}