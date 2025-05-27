/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.actions.NewElementAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2025-05-27
 */
@ActionImpl(id = "ProjectNewElementAction", shortcutFrom = @ActionRef(type = NewElementAction.class))
public class ProjectNewElementAction extends NewElementAction {
    @Inject
    public ProjectNewElementAction(ActionManager actionManager) {
        super(actionManager);

        Presentation presentation = getTemplatePresentation();
        LocalizeValue text = ProjectUIViewLocalize.actionNewelementProjectviewText();
        presentation.setTextValue(text);
        presentation.setDescriptionValue(text);
        presentation.setIcon(ImageEffects.layered(PlatformIconGroup.generalAdd(), PlatformIconGroup.generalDropdown()));
    }

    @Override
    protected void showPopup(AnActionEvent event) {
        createPopup(event.getDataContext()).showUnderneathOf(event.getInputEvent().getComponent());
    }

    @Override
    protected boolean isEnabled(@Nonnull AnActionEvent e, @Nonnull IdeView ideView) {
        ToolWindow toolWindow = e.getData(ToolWindow.KEY);
        if (toolWindow == null) {
            return false;
        }
        
        if (ToolWindowId.PROJECT_VIEW.equals(toolWindow.getId())) {
            if (ideView.getDirectories().length == 0) {
                return false;
            }
        }
        return true;
    }
}
