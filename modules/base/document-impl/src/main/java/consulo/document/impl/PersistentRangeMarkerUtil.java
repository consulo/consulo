/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.document.impl;

import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRangeScalarUtil;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 12/27/10 4:26 PM
 */
public class PersistentRangeMarkerUtil {
    /**
     * Answers if document region identified by the given range marker should be translated via diff algorithm on document change
     * identified by the given event.
     *
     * @param e     event that describes document change
     * @param range target range marker range for which update strategy should be selected
     * @return {@code true} if target document range referenced by the given range marker should be translated via
     * diff algorithm; {@code false} otherwise
     */
    public static boolean shouldTranslateViaDiff(@Nonnull DocumentEvent e, long range) {
        if (e.isWholeTextReplaced()) {
            // Perform translation if the whole text is replaced.
            return true;
        }
        if (e.getOffset() >= TextRangeScalarUtil.endOffset(range) || e.getOffset() + e.getOldLength() <= TextRangeScalarUtil.startOffset(range)) {
            // Don't perform complex processing if the change doesn't affect the target range.
            return false;
        }

        // Perform complex processing only if at least 80% of the document was updated.
        return Math.max(e.getNewLength(), e.getOldLength()) * 5 >= e.getDocument().getTextLength() * 4;
    }
}
