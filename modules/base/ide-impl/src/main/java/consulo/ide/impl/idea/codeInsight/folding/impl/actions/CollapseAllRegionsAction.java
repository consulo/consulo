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

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.action.EditorAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ActionImpl(id = "CollapseAllRegions")
public class CollapseAllRegionsAction extends EditorAction {
    public CollapseAllRegionsAction() {
        super(
            ActionLocalize.actionCollapseallregionsText(),
            ActionLocalize.actionCollapseallregionsDescription(),
            new BaseFoldingHandler() {
                @Override
                public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
                    Project project = editor.getProject();
                    assert project != null;
                    PsiDocumentManager.getInstance(project).commitAllDocuments();
                    CodeFoldingManager.getInstance(project).updateFoldRegions(editor);

                    List<FoldRegion> regions = getFoldRegionsForSelection(editor, caret);
                    editor.getFoldingModel().runBatchFoldingOperation(() -> {
                        for (FoldRegion region : regions) {
                            region.setExpanded(false);
                        }
                    });
                }
            }
        );
    }
}
