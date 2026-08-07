/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.codeEditor.internal;

import consulo.document.Document;
import consulo.ui.clipboard.DataTransferType;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Text put on the kill ring together with the place of the document it was taken from.
 * <p>
 * The place is what makes adjacent kills combine into a single unit the way the
 * <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Kill-Ring.html#Kill-Ring">emacs kill ring</a>
 * does - subsequent 'cut to line end' calls paste as one block.
 * <p>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-04-15
 */
public class KillRingData {
    public static final DataTransferType<KillRingData> TYPE = DataTransferType.create("consulo.editor.killRing");

    private final String myText;
    private final WeakReference<Document> myDocument;
    private final int myStartOffset;
    private final int myEndOffset;
    private final boolean myCut;

    public KillRingData(String text, Document document, int startOffset, int endOffset, boolean cut) {
        myText = text;
        myDocument = new WeakReference<>(document);
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myCut = cut;
    }

    public String getText() {
        return myText;
    }

    public @Nullable Document getDocument() {
        return myDocument.get();
    }

    public int getStartOffset() {
        return myStartOffset;
    }

    /**
     * @return offset of the target text end on the moment of the current object construction
     */
    public int getEndOffset() {
        return myEndOffset;
    }

    public boolean isCut() {
        return myCut;
    }

    /**
     * Combines two adjacent kills of the same document into one, the way the emacs kill ring does.
     *
     * @return the combined kill, or {@code null} when the two are not adjacent
     */
    public static @Nullable KillRingData merge(KillRingData newData, KillRingData oldData) {
        Document document = newData.getDocument();
        if (document == null || document != oldData.getDocument()) {
            return null;
        }

        if (oldData.isCut() && newData.getStartOffset() == oldData.getStartOffset()) {
            return new KillRingData(
                oldData.getText() + newData.getText(),
                document,
                oldData.getStartOffset(),
                newData.getEndOffset(),
                newData.isCut()
            );
        }

        if (newData.getStartOffset() == oldData.getEndOffset()) {
            return new KillRingData(
                oldData.getText() + newData.getText(),
                document,
                oldData.getStartOffset(),
                newData.getEndOffset(),
                false
            );
        }

        if (newData.getEndOffset() == oldData.getStartOffset()) {
            return new KillRingData(
                newData.getText() + oldData.getText(),
                document,
                newData.getStartOffset(),
                oldData.getEndOffset(),
                false
            );
        }

        return null;
    }

    @Override
    public String toString() {
        return "data='" + myText + "', startOffset=" + myStartOffset + ", endOffset=" + myEndOffset + ", cut=" + myCut;
    }
}
