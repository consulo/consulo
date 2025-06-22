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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.DocumentRunnable;
import consulo.ide.impl.idea.openapi.ide.KillRingTransferable;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Stands for emacs <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Other-Kill-Commands.html">kill-ring-save</a> command.
 * <p/>
 * Generally, it puts currently selected text to the {@link KillRingTransferable kill ring}.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-04-19
 */
public class KillRingSaveAction extends TextComponentEditorAction {
    public KillRingSaveAction() {
        super(new Handler(false));
    }

    static class Handler extends EditorActionHandler {

        private final boolean myRemove;

        Handler(boolean remove) {
            myRemove = remove;
        }

        @Override
        @RequiredUIAccess
        public void execute(final Editor editor, DataContext dataContext) {
            SelectionModel selectionModel = editor.getSelectionModel();
            if (!selectionModel.hasSelection()) {
                return;
            }

            final int start = selectionModel.getSelectionStart();
            final int end = selectionModel.getSelectionEnd();
            if (start >= end) {
                return;
            }
            KillRingUtil.copyToKillRing(editor, start, end, false);
            if (myRemove) {
                Application.get().runWriteAction(new DocumentRunnable(editor.getDocument(), editor.getProject()) {
                    @Override
                    public void run() {
                        editor.getDocument().deleteString(start, end);
                    }
                });
            }
        }
    }
}
