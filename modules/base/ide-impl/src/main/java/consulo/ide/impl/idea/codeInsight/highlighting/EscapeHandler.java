/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.dataContext.DataContext;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.language.editor.highlight.HighlightManager;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

@ExtensionImpl(order = "after hide-hints")
public class EscapeHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    public void execute(Editor editor, DataContext dataContext) {
        editor.setHeaderComponent(null);

        Project project = dataContext.getData(Project.KEY);
        if (project != null) {
            HighlightManagerImpl highlightManager = (HighlightManagerImpl) HighlightManager.getInstance(project);
            if (highlightManager != null
                && highlightManager.hideHighlights(editor, HighlightManager.HIDE_BY_ESCAPE | HighlightManager.HIDE_BY_ANY_KEY)) {
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.setInfo("");
                }

                FindManager findManager = FindManager.getInstance(project);
                if (findManager != null) {
                    FindModel model = findManager.getFindNextModel(editor);
                    if (model != null) {
                        model.setSearchHighlighters(false);
                        findManager.setFindNextModel(model);
                    }
                }

                return;
            }
        }

        myOriginalHandler.execute(editor, dataContext);
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
        if (editor.hasHeaderComponent()) {
            return true;
        }
        Project project = dataContext.getData(Project.KEY);

        if (project != null) {
            HighlightManagerImpl highlightManager = (HighlightManagerImpl) HighlightManager.getInstance(project);
            Map<RangeHighlighter, HighlightManagerImpl.HighlightInfoFlags> map = highlightManager.getHighlightInfoMap(editor, false);
            if (map != null) {
                for (HighlightManagerImpl.HighlightInfoFlags info : map.values()) {
                    if (!info.editor().equals(editor)) {
                        continue;
                    }
                    if ((info.flags() & HighlightManager.HIDE_BY_ESCAPE) != 0) {
                        return true;
                    }
                }
            }
        }

        return myOriginalHandler.isEnabled(editor, dataContext);
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ESCAPE;
    }
}
