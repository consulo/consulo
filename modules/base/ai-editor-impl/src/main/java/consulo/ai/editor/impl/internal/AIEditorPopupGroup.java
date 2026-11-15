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

import consulo.ai.AIProviderTable;
import consulo.ai.editor.impl.internal.review.CommentOnPlanAction;
import consulo.ai.editor.impl.internal.setting.AIEditorSettings;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * The AI actions of the editor popup - shown while an AI provider is set up and the settings allow it.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ActionImpl(
    id = AIEditorPopupGroup.ID,
    children = {
        @ActionRef(type = AddSelectionToAIChatAction.class),
        @ActionRef(type = CommentForAIAction.class),
        @ActionRef(type = CommentOnPlanAction.class)
    },
    parents = @ActionParentRef(
        value = @ActionRef(id = "EditorLangPopupMenu"),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = "EditorPopupMenu.Run")
    )
)
public class AIEditorPopupGroup extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
    public static final String ID = "AI.EditorPopupGroup";

    public AIEditorPopupGroup() {
        super(AIEditorLocalize.groupAiEditorPopupText(), true);
        getTemplatePresentation().setIcon(PlatformIconGroup.toolwindowsToolwindowaskai());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(
            e.getData(Project.KEY) != null
                && e.getData(Editor.KEY) != null
                && AIEditorSettings.getInstance().isShowEditorPopupGroup()
                && !AIProviderTable.getInstance().getProviders().isEmpty()
        );
    }
}
