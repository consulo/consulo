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
package consulo.codeEditor.impl;

import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.PersistentRangeMarkerUtil;
import consulo.document.impl.event.DocumentEventImpl;
import consulo.document.internal.DocumentEx;
import consulo.document.util.DocumentUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implementation of the markup element for the editor and document.
 *
 * @author max
 */
class PersistentRangeHighlighterImpl extends RangeHighlighterImpl {
    private int myLine; // for PersistentRangeHighlighterImpl only

    static PersistentRangeHighlighterImpl create(@Nonnull MarkupModelImpl model,
                                                 int offset,
                                                 int layer,
                                                 @Nonnull HighlighterTargetArea target,
                                                 @Nullable TextAttributesKey textAttributes,
                                                 boolean normalizeStartOffset) {
        int line = model.getDocument().getLineNumber(offset);
        int startOffset = normalizeStartOffset ? model.getDocument().getLineStartOffset(line) : offset;
        return new PersistentRangeHighlighterImpl(model, startOffset, line, layer, target, textAttributes);
    }

    private PersistentRangeHighlighterImpl(@Nonnull MarkupModelImpl model,
                                           int startOffset,
                                           int line,
                                           int layer,
                                           @Nonnull HighlighterTargetArea target,
                                           @Nullable TextAttributesKey textAttributes) {
        super(model, startOffset, model.getDocument().getLineEndOffset(line), layer, target, textAttributes, false, false);

        myLine = line;
    }

    @Override
    protected void changedUpdateImpl(@Nonnull DocumentEvent e) {
        // todo Denis Zhdanov
        DocumentEventImpl event = (DocumentEventImpl) e;
        final boolean shouldTranslateViaDiff = isValid() && PersistentRangeMarkerUtil.shouldTranslateViaDiff(event, getStartOffset(), getEndOffset());
        boolean wasTranslatedViaDiff = shouldTranslateViaDiff;
        if (shouldTranslateViaDiff) {
            wasTranslatedViaDiff = translatedViaDiff(e, event);
        }
        if (!wasTranslatedViaDiff) {
            super.changedUpdateImpl(e);
            if (isValid()) {
                myLine = getDocument().getLineNumber(getStartOffset());
                int endLine = getDocument().getLineNumber(getEndOffset());
                if (endLine != myLine) {
                    setIntervalEnd(getDocument().getLineEndOffset(myLine));
                }
            }
        }
        if (isValid() && getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE) {
            setIntervalStart(DocumentUtil.getFirstNonSpaceCharOffset(getDocument(), myLine));
            setIntervalEnd(getDocument().getLineEndOffset(myLine));
        }
    }

    private boolean translatedViaDiff(DocumentEvent e, DocumentEventImpl event) {
        try {
            myLine = event.translateLineViaDiff(myLine);
        }
        catch (FilesTooBigForDiffException ignored) {
            return false;
        }
        if (myLine < 0 || myLine >= getDocument().getLineCount()) {
            invalidate(e);
        }
        else {
            DocumentEx document = getDocument();
            setIntervalStart(document.getLineStartOffset(myLine));
            setIntervalEnd(document.getLineEndOffset(myLine));
        }
        return true;
    }

    @Override
    public String toString() {
        return "PersistentRangeHighlighter" +
            (isGreedyToLeft() ? "[" : "(") +
            (isValid() ? "valid" : "invalid") + "," + getStartOffset() + "," + getEndOffset() + " - " + myLine +
            (isGreedyToRight() ? "]" : ")");
    }
}
