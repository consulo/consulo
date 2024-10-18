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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.document.util.TextRange;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.editor.EditorLastActionTracker;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.language.editor.action.SelectWordUtil;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nullable;

abstract public class SelectOccurrencesActionHandler extends EditorActionHandler {
    private static final Key<Boolean> NOT_FOUND = Key.create("select.next.occurence.not.found");
    private static final Key<Boolean> WHOLE_WORDS = Key.create("select.next.occurence.whole.words");

    protected static void setSelection(Editor editor, Caret caret, TextRange selectionRange) {
        EditorActionUtil.makePositionVisible(editor, selectionRange.getStartOffset());
        EditorActionUtil.makePositionVisible(editor, selectionRange.getEndOffset());
        caret.setSelection(selectionRange.getStartOffset(), selectionRange.getEndOffset());
    }

    @RequiredUIAccess
    protected static void showHint(final Editor editor) {
        LocalizeValue message = FindLocalize.selectNextOccurenceNotFoundMessage();
        final LightweightHint hint = new LightweightHint(HintUtil.createInformationLabel(message.get()));
        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            HintManager.UNDER,
            HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING,
            0,
            false
        );
    }

    protected static boolean getAndResetNotFoundStatus(Editor editor) {
        boolean status = editor.getUserData(NOT_FOUND) != null;
        editor.putUserData(NOT_FOUND, null);
        return status && isRepeatedActionInvocation();
    }

    protected static void setNotFoundStatus(Editor editor) {
        editor.putUserData(NOT_FOUND, Boolean.TRUE);
    }

    protected static boolean isWholeWordSearch(Editor editor) {
        if (!isRepeatedActionInvocation()) {
            editor.putUserData(WHOLE_WORDS, null);
        }
        Boolean value = editor.getUserData(WHOLE_WORDS);
        return value != null;
    }

    @Nullable
    protected static TextRange getSelectionRange(Editor editor, Caret caret) {
        return SelectWordUtil.getWordSelectionRange(
            editor.getDocument().getCharsSequence(),
            caret.getOffset(),
            SelectWordUtil.JAVA_IDENTIFIER_PART_CONDITION
        );
    }

    protected static void setWholeWordSearch(Editor editor, boolean isWholeWordSearch) {
        editor.putUserData(WHOLE_WORDS, isWholeWordSearch);
    }

    protected static boolean isRepeatedActionInvocation() {
        String lastActionId = EditorLastActionTracker.getInstance().getLastActionId();
        return IdeActions.ACTION_SELECT_NEXT_OCCURENCE.equals(lastActionId)
            || IdeActions.ACTION_UNSELECT_PREVIOUS_OCCURENCE.equals(lastActionId);
    }
}
