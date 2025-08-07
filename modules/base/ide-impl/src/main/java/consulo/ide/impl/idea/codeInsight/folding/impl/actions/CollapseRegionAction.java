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
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.action.EditorAction;
import consulo.platform.base.localize.ActionLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "CollapseRegion")
public class CollapseRegionAction extends EditorAction {
    public CollapseRegionAction() {
        super(
            ActionLocalize.actionCollapseregionText(),
            ActionLocalize.actionCollapseregionDescription(),
            new BaseFoldingHandler() {
                @Override
                public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
                    CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(editor.getProject());
                    foldingManager.updateFoldRegions(editor);

                    int line = editor.getCaretModel().getLogicalPosition().line;

                    Runnable processor = () -> {
                        FoldRegion region = FoldingUtil.findFoldRegionStartingAtLine(editor, line);
                        if (region != null && region.isExpanded()) {
                            region.setExpanded(false);
                        }
                        else {
                            int offset = editor.getCaretModel().getOffset();
                            FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, offset);
                            for (FoldRegion region1 : regions) {
                                if (region1.isExpanded()) {
                                    region1.setExpanded(false);
                                    break;
                                }
                            }
                        }
                    };
                    editor.getFoldingModel().runBatchFoldingOperation(processor);
                }
            });
    }
}
