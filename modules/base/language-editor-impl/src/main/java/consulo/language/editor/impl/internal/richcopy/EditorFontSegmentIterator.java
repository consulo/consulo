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

import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import consulo.ui.impl.font.TypefaceImpl;
import org.jspecify.annotations.Nullable;

/**
 * Keeps every range whole and names the editor font for it, without looking for a font which displays its characters - that needs
 * the fonts of a frontend, and whoever shows the markup falls back by itself
 */
public final class EditorFontSegmentIterator implements FontSegmentIterator {
    private final WholeRangeFontSegmentIterator myIterator = new WholeRangeFontSegmentIterator();
    private final Typeface myTypeface;

    /**
     * @param editorFontName the font is taken for monospaced when the frontend does not know it, as an editor font is
     */
    public EditorFontSegmentIterator(String editorFontName) {
        Typeface typeface = FontManager.get().findTypeface(editorFontName);
        myTypeface = typeface == null ? new TypefaceImpl(editorFontName, true) : typeface;
    }

    @Override
    public void start(CharSequence text, int startOffset, int endOffset, int fontStyle) {
        myIterator.start(text, startOffset, endOffset, fontStyle);
    }

    @Override
    public boolean atEnd() {
        return myIterator.atEnd();
    }

    @Override
    public void advance() {
        myIterator.advance();
    }

    @Override
    public int getStart() {
        return myIterator.getStart();
    }

    @Override
    public int getEnd() {
        return myIterator.getEnd();
    }

    @Override
    public @Nullable Typeface getTypeface() {
        return myTypeface;
    }
}
