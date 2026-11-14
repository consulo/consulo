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
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarActions;
import consulo.document.Document;
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
 * Attaches the selected code to the next message of the AI chat - the file it is in, with the lines - or the file
 * when nothing is selected.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ActionImpl(
    id = "AI.AddSelectionToChat",
    parents = {
        @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)),
        @ActionParentRef(@ActionRef(id = CodeFloatingToolbarActions.GROUP))
    }
)
public class AddSelectionToAIChatAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public AddSelectionToAIChatAction() {
        super(AIEditorLocalize.actionAddSelectionText(), AIEditorLocalize.actionAddSelectionDescription(), AIIconGroup.toolwindowAiassistant());
    }

    @Override
    public void update(AnActionEvent e) {
        // the selection is read on the ui thread only - the action takes the file when nothing is selected
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && e.getData(Editor.KEY) != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            return;
        }
        SelectionModel selection = editor.getSelectionModel();
        String text = selection.getSelectedText();
        VirtualFile file = editor.getVirtualFile();
        if (text == null || text.isBlank()) {
            if (file != null && file.isInLocalFileSystem()) {
                AIContent link = AIContent.resourceLink("file://" + file.getPath(), file.getName(), null);
                AIChatContextManager.getInstance(project).attach(new AIChatAttachment(LocalizeValue.of(file.getName()), List.of(link)));
            }
            return;
        }
        Document document = editor.getDocument();
        int firstLine = document.getLineNumber(selection.getSelectionStart()) + 1;
        int lastLine = document.getLineNumber(Math.max(selection.getSelectionStart(), selection.getSelectionEnd() - 1)) + 1;
        String path = file == null ? "selection" : file.getPath();
        String name = file == null ? path : file.getName();

        LocalizeValue label = AIEditorLocalize.labelSelection(name, firstLine, lastLine);
        AIContent selected = AIContent.resource(
            "file://" + path + "#L" + firstLine + "-L" + lastLine,
            null,
            text
        );
        AIChatContextManager.getInstance(project).attach(new AIChatAttachment(label, List.of(selected)));
    }
}
