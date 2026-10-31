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
 * Keeps every range whole and names no font: the markup leaves the font to whoever shows it
 */
public final class WholeRangeFontSegmentIterator implements FontSegmentIterator {
    private int myStart;
    private int myEnd;
    private boolean myAtEnd = true;

    @Override
    public void start(CharSequence text, int startOffset, int endOffset, int fontStyle) {
        myStart = startOffset;
        myEnd = endOffset;
        myAtEnd = startOffset >= endOffset;
    }

    @Override
    public boolean atEnd() {
        return myAtEnd;
    }

    @Override
    public void advance() {
        myAtEnd = true;
    }

    @Override
    public int getStart() {
        return myStart;
    }

    @Override
    public int getEnd() {
        return myEnd;
    }

    @Override
    public @Nullable Typeface getTypeface() {
        return null;
    }
}
