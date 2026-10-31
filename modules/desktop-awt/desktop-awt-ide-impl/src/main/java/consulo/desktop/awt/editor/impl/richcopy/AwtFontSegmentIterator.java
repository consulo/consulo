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
package consulo.desktop.awt.editor.impl.richcopy;

import consulo.colorScheme.internal.FontPreferences;
import consulo.ide.impl.idea.openapi.editor.impl.FontFallbackIterator;
import consulo.language.editor.impl.internal.richcopy.FontSegmentIterator;
import consulo.ui.font.Typeface;
import consulo.ui.impl.font.TypefaceImpl;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Splits a range by the editor font which can display its characters, falling back to other fonts the way the editor does, and
 * names each font as other applications know it
 */
public final class AwtFontSegmentIterator implements FontSegmentIterator {
    private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, false, false);
    private static final Map<String, Boolean> ourMonospaced = new ConcurrentHashMap<>();

    private final FontFallbackIterator myIterator = new FontFallbackIterator();

    public AwtFontSegmentIterator(FontPreferences fontPreferences) {
        myIterator.setPreferredFonts(fontPreferences);
    }

    @Override
    public void start(CharSequence text, int startOffset, int endOffset, int fontStyle) {
        myIterator.setFontStyle(fontStyle);
        myIterator.start(text, startOffset, endOffset);
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
        Font font = myIterator.getFont();
        String name = FontMapper.getPhysicalFontName(font.getFamily());
        return new TypefaceImpl(name, ourMonospaced.computeIfAbsent(name, it -> isMonospaced(font)));
    }

    private static boolean isMonospaced(Font font) {
        return font.getStringBounds("l", FONT_RENDER_CONTEXT).getWidth() == font.getStringBounds("W", FONT_RENDER_CONTEXT).getWidth();
    }
}
