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

import consulo.ai.editor.impl.internal.plan.AIPlanReviewManager;
import consulo.ai.editor.impl.internal.plan.CommentOnPlanAction;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarConfiguration;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarProvider;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarSelection;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The selection toolbar of text no language fills one for - plain text, the markdown plans of the agents, logs - with
 * the actions of the AI chat. A language which has a toolbar of its own shows the AI actions in it, ahead of this.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl(order = "last")
public class AICodeFloatingToolbarProvider implements CodeFloatingToolbarProvider {
    private static final List<String> PLAN_ACTIONS = List.of(CommentOnPlanAction.ID, AddSelectionToAIChatAction.ID);
    private static final List<String> ACTIONS = List.of(AddSelectionToAIChatAction.ID, CommentForAIFieldAction.ID);

    @RequiredReadAction
    @Override
    public @Nullable CodeFloatingToolbarConfiguration getConfiguration(Editor editor, CodeFloatingToolbarSelection selection) {
        Project project = editor.getProject();
        VirtualFile file = editor.getVirtualFile();
        if (!selection.hasSelection() || project == null || file == null || editor.getEditorKind() != EditorKind.MAIN_EDITOR
            || editor.isViewer() || editor.isOneLineMode()) {
            return null;
        }
        // the selected lines of a plan take a review comment - the plan goes back to the agent with the comments
        if (AIPlanReviewManager.getInstance(project).isPlan(file)) {
            return new CodeFloatingToolbarConfiguration(PLAN_ACTIONS, true);
        }
        return new CodeFloatingToolbarConfiguration(ACTIONS, true);
    }
}
