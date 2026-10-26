// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.comment;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.BackspaceHandlerDelegate;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;

/**
 * Handles backspace in Markdown review comments to force showing a completion popup when a user edits a mention.
 * For example, we have some completion variants for the prefix "@a" and nothing for the prefix "@ab".
 * You type "@ab", the completion dropdown disappears, and then you press backspace to remove the last character.
 * The popup should reappear then, so the handler is responsible for reopening the popup.
 */
public abstract class CommentBackspaceHandler<T> extends BackspaceHandlerDelegate {
    @Override
    public void beforeCharDeleted(char c, PsiFile file, Editor editor) {
    }

    @Override
    public boolean charDeleted(char c, PsiFile file, Editor editor) {
        Project project = editor.getProject();
        if (project == null) {
            return false;
        }
        if (editor.getUserData(getKey()) == null) {
            return false;
        }

        if (LookupManager.getActiveLookup(editor) == null && isCaretOnUserMention(editor)) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        }

        return false;
    }

    /**
     * @return an instance of a user data key that marks a review comment editor
     */
    public abstract Key<T> getKey();

    /**
     * @return true if the character is a valid mention character for this comment
     */
    public abstract boolean isValidMentionCharacter(char c);

    private boolean isCaretOnUserMention(Editor editor) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();

        CharSequence chars = document.getCharsSequence();
        int i = offset;
        while (i > 0 && isValidMentionCharacter(chars.charAt(i - 1))) {
            i--;
        }
        return i < offset && chars.charAt(i) == '@';
    }
}
