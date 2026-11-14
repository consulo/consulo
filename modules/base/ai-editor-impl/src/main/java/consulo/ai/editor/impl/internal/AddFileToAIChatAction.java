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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatAttachment;
import consulo.ai.AIChatContextManager;
import consulo.ai.AIContent;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.ai.icon.AIIconGroup;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;

/**
 * Attaches the selected files to the next message of the AI chat - as links the agent reads itself.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ActionImpl(id = "AI.AddFileToChat", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP)))
public class AddFileToAIChatAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public AddFileToAIChatAction() {
        super(AIEditorLocalize.actionAddFileText(), AIEditorLocalize.actionAddFileDescription(), AIIconGroup.toolwindowAiassistant());
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && files != null && files.length > 0);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (project == null || files == null) {
            return;
        }
        for (VirtualFile file : files) {
            if (!file.isInLocalFileSystem()) {
                continue;
            }
            AIContent link = AIContent.resourceLink("file://" + file.getPath(), file.getName(), null);
            AIChatContextManager.getInstance(project).attach(new AIChatAttachment(LocalizeValue.of(file.getName()), List.of(link)));
        }
    }
}
