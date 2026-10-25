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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.localHistory.ActivityId;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.undoRedo.CommandProcessor;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ActionImpl(id = "SandEditWithActivity", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class SandEditWithActivityAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public static final String KIND = "Edit";

    public SandEditWithActivityAction() {
        super(LocalizeValue.localizeTODO("Sand: Edit With Local History Activity"));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            return;
        }

        Document document = editor.getDocument();
        LocalizeValue name = LocalizeValue.localizeTODO("Sand Activity Edit");
        LocalHistoryAction action = LocalHistory.getInstance().startAction(name, new ActivityId(SandActivityPresentationProvider.ID, KIND));
        try {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(name)
                .inWriteAction()
                .run(() -> document.insertString(0, "sand activity\n"));
        }
        finally {
            action.finish();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && e.hasData(Editor.KEY));
    }
}
