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
package consulo.ide.impl.idea.ide.actions;

import consulo.fileEditor.action.CloseEditorsActionBase;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorWindow;

public class CloseAllUnmodifiedEditorsAction extends CloseEditorsActionBase {
    @Override
    protected boolean isFileToClose(FileEditorComposite editor, FileEditorWindow window) {
        return !window.getManager().isChanged(editor);
    }

    @Override
    protected boolean isActionEnabled(Project project, AnActionEvent event) {
        return super.isActionEnabled(project, event) && ProjectLevelVcsManager.getInstance(project).getAllActiveVcss().length > 0;
    }

    @Override
    protected String getPresentationText(boolean inSplitter) {
        if (inSplitter) {
            return IdeLocalize.actionCloseAllUnmodifiedEditorsInTabGroup().get();
        }
        else {
            return IdeLocalize.actionCloseAllUnmodifiedEditors().get();
        }
    }
}
