/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.versionSystemControl;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.impl.FontFallbackIterator;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTracker;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTrackerDrawing;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.versionControlSystem.internal.VersionControlSystemInternal;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceImpl
@Singleton
public class DesktopAWTVersionControlSystemInternalImpl implements VersionControlSystemInternal {
    @Override
    public String getHtmlWithFonts(@Nonnull String input, int style, @Nonnull Font baseFont) {
        int start = baseFont.canDisplayUpTo(input);
        if (start == -1) {
            return input;
        }

        StringBuilder result = new StringBuilder();

        FontFallbackIterator it = new FontFallbackIterator();
        it.setPreferredFont(baseFont.getFamily(), baseFont.getSize());
        it.setFontStyle(style);

        it.start(input, 0, input.length());
        while (!it.atEnd()) {
            Font font = it.getFont();

            boolean insideFallbackBlock = !font.getFamily().equals(baseFont.getFamily());
            if (insideFallbackBlock) {
                result.append("<font face=\"").append(font.getFamily()).append("\">");
            }

            result.append(input, it.getStart(), it.getEnd());

            if (insideFallbackBlock) {
                result.append("</font>");
            }

            it.advance();
        }

        return result.toString();
    }

    @Override
    public void moveToRange(VcsRange range, Editor editor, LineStatusTrackerI tracker) {
        LineStatusTrackerDrawing.moveToRange(range, editor, (LineStatusTracker) tracker);
    }
}
