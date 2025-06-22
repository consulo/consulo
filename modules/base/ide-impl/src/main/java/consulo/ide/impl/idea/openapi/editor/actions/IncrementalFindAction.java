/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nullable;

public class IncrementalFindAction extends EditorAction {
    public static final Key<Boolean> SEARCH_DISABLED = Key.create("EDITOR_SEARCH_DISABLED");

    public static class Handler extends EditorActionHandler {

        private final boolean myReplace;

        public Handler(boolean isReplace) {
            myReplace = isReplace;
        }

        @Override
        public void execute(Editor editor, DataContext dataContext) {
            Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(Project.KEY);
            if (!editor.isOneLineMode()) {
                EditorSearchSession search = EditorSearchSession.get(editor);
                if (search != null) {
                    search.getComponent().requestFocusInTheSearchFieldAndSelectContent(project);
                    FindUtil.configureFindModel(myReplace, editor, search.getFindModel(), false);
                }
                else {
                    FindManager findManager = FindManager.getInstance(project);
                    FindModel model;
                    if (myReplace) {
                        model = findManager.createReplaceInFileModel();
                    }
                    else {
                        model = new FindModel();
                        model.copyFrom(findManager.getFindInFileModel());
                    }
                    boolean consoleViewEditor = ConsoleViewUtil.isConsoleViewEditor(editor);
                    FindUtil.configureFindModel(myReplace, editor, model, consoleViewEditor);
                    EditorSearchSession.start(editor, model, project).getComponent()
                        .requestFocusInTheSearchFieldAndSelectContent(project);
                    if (!consoleViewEditor && editor.getSelectionModel().hasSelection()) {
                        // selection is used as string to find without search model modification so save the pattern explicitly
                        FindUtil.updateFindInFileModel(project, model, true);
                    }
                }
            }
        }

        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            if (myReplace && ConsoleViewUtil.isConsoleViewEditor(editor) &&
                !ConsoleViewUtil.isReplaceActionEnabledForConsoleViewEditor(editor)) {
                return false;
            }
            if (SEARCH_DISABLED.get(editor, false)) {
                return false;
            }
            Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(Project.KEY);
            return project != null && !editor.isOneLineMode();
        }
    }

    public IncrementalFindAction() {
        super(new Handler(false));
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsSearch();
    }
}