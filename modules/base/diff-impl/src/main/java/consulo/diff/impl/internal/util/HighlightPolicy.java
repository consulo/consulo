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

import jakarta.annotation.Nonnull;

public enum HighlightPolicy {
    DO_NOT_HIGHLIGHT("Do not highlight"),
    BY_LINE("Highlight lines"),
    BY_WORD("Highlight words"),
    BY_WORD_SPLIT("Highlight split changes");

    @Nonnull
    private final String myText;

    HighlightPolicy(@Nonnull String text) {
        myText = text;
    }

    @Nonnull
    public String getText() {
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
