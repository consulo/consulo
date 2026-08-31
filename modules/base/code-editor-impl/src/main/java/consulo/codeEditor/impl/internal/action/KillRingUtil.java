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
package consulo.codeEditor.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.internal.KillRingData;
import consulo.document.Document;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.internal.CopyPasteManagerInternal;
import consulo.util.lang.StringUtil;

import java.util.List;

/**
 * Holds utility methods for {@link KillRingData kill ring}-aware processing.
 */
public class KillRingUtil {
    private KillRingUtil() {
    }

    /**
     * Cuts target region from the given editor and puts it to the kill ring.
     *
     * @param editor target editor
     * @param start  start offset of the target text region within the given editor (inclusive)
     * @param end    end offset of the target text region within the given editor (exclusive)
     */
    public static void cut(Editor editor, int start, int end) {
        copyToKillRing(editor, start, end, true);
        editor.getDocument().deleteString(start, end);
    }

    /**
     * Copies target region from the given offset to the kill ring, i.e. combines it with the previously
     * copied/cut adjacent text if necessary and puts to the clipboard.
     *
     * @param editor      target editor
     * @param startOffset start offset of the target region within the given editor
     * @param endOffset   end offset of the target region within the given editor
     * @param cut         flag that identifies if target text region will be cut from the given editor
     */
    public static void copyToKillRing(Editor editor, int startOffset, int endOffset, boolean cut) {
        Document document = editor.getDocument();
        String s = document.getCharsSequence().subSequence(startOffset, endOffset).toString();
        s = StringUtil.convertLineSeparators(s);

        CopyPasteManager manager = CopyPasteManager.getInstance();
        KillRingData data = new KillRingData(s, document, startOffset, endOffset, cut);

        KillRingData previous = manager.getLocalContents().get(KillRingData.TYPE);
        CopyPasteManagerInternal history = (CopyPasteManagerInternal) manager;
        if (previous != null && !history.isKillRingBroken()) {
            KillRingData merged = KillRingData.merge(data, previous);
            if (merged != null) {
                List<DataTransfer> entries = history.getHistory();
                // replace the previous kill rather than stacking next to it - but only when it is
                // still the newest entry, identity on the payload says it is the same kill
                if (!entries.isEmpty() && entries.get(0).get(KillRingData.TYPE) == previous) {
                    history.removeFromHistory(entries.get(0));
                }
                data = merged;
                s = merged.getText();
            }
        }

        manager.setContents(DataTransfer.builder()
            .put(DataTransferType.TEXT, s)
            .put(KillRingData.TYPE, data)
            .build());

        if (editor instanceof EditorEx editorEx) {
            if (editorEx.isStickySelection()) {
                editorEx.setStickySelection(false);
            }
        }
    }
}