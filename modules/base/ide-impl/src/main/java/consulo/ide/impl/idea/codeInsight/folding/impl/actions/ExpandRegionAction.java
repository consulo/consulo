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

package consulo.ide.impl.idea.codeInsight.folding.impl.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ExpandRegionAction extends EditorAction {
    public ExpandRegionAction() {
        super(new BaseFoldingHandler() {
            @Override
            public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
                expandRegionAtCaret(editor.getProject(), editor);
            }
        });
    }

    private static void expandRegionAtCaret(final Project project, @Nullable final Editor editor) {
        if (editor == null) {
            return;
        }

        FoldingUtil.expandRegionAtOffset(project, editor, editor.getCaretModel().getOffset());
    }
}
