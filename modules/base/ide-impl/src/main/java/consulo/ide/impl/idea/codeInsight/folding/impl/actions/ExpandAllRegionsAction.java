/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.language.editor.folding.CodeFoldingManager;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.action.EditorAction;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.util.List;

public class ExpandAllRegionsAction extends EditorAction {
    public ExpandAllRegionsAction() {
        super(new BaseFoldingHandler() {
            @Override
            public void doExecute(@Nonnull final Editor editor, @Nullable Caret caret, DataContext dataContext) {
                Project project = editor.getProject();
                assert project != null;
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                CodeFoldingManager codeFoldingManager = CodeFoldingManager.getInstance(project);
                codeFoldingManager.updateFoldRegions(editor);

                final List<FoldRegion> regions = getFoldRegionsForSelection(editor, caret);
                editor.getFoldingModel().runBatchFoldingOperation(() -> {
                    boolean anythingDone = false;
                    for (FoldRegion region : regions) {
                        // try to restore to default state at first
                        Boolean collapsedByDefault = codeFoldingManager.isCollapsedByDefault(region);
                        if (!region.isExpanded() && (collapsedByDefault == null || !collapsedByDefault)) {
                            region.setExpanded(true);
                            anythingDone = true;
                        }
                    }

                    if (!anythingDone) {
                        for (FoldRegion region : regions) {
                            region.setExpanded(true);
                        }
                    }

                });
            }
        });
    }
}
