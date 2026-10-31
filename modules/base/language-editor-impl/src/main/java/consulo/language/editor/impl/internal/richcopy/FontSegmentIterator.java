/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.richcopy;

import consulo.ui.font.Typeface;
import org.jspecify.annotations.Nullable;

/**
 * Splits a range of text with the same attributes into segments, each displayed by one font family. After {@link #start} the
 * first segment is current.
 */
public interface FontSegmentIterator {
    void start(CharSequence text, int startOffset, int endOffset, int fontStyle);

    boolean atEnd();

    void advance();

    int getStart();

    int getEnd();

    /**
     * @return the family of the current segment as other applications name it, or {@code null} when the markup names no font
     */
    @Nullable Typeface getTypeface();
}
