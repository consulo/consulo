/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import java.awt.event.InputEvent;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "WelcomeScreen.OpenSelected")
public class OpenSelectedProjectsAction extends RecentProjectsWelcomeScreenActionBase {
    public OpenSelectedProjectsAction() {
        super(ProjectUILocalize.actionRecentProjectsOpenSelectedProjectText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        List<AnAction> elements = getSelectedElements(e);
        e = new AnActionEvent(
            e.getInputEvent(),
            e.getDataContext(),
            e.getPlace(),
            e.getPresentation(),
            e.getActionManager(),
            InputEvent.SHIFT_MASK
        );
        for (AnAction element : elements) {
            if (element instanceof PopupProjectGroupActionGroup popupGroup) {
                for (AnAction action : popupGroup.getChildren(e)) {
                    action.actionPerformed(e);
                }
            }
            else {
                element.actionPerformed(e);
            }
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        List<AnAction> selectedElements = getSelectedElements(e);
        boolean hasProject = false;
        boolean hasGroup = false;
        for (AnAction element : selectedElements) {
            if (element instanceof ReopenProjectAction) {
                hasProject = true;
            }
            if (element instanceof PopupProjectGroupActionGroup) {
                hasGroup = true;
            }

            if (hasGroup && hasProject) {
                e.getPresentation().setEnabled(false);
                return;
            }
        }
        if (ActionPlaces.WELCOME_SCREEN.equals(e.getPlace())) {
            presentation.setEnabledAndVisible(true);
            if (selectedElements.size() == 1 && selectedElements.get(0) instanceof PopupProjectGroupActionGroup) {
                presentation.setTextValue(ProjectUILocalize.actionRecentProjectsOpenSelectedProjectsText());
            }
            else {
                presentation.setTextValue(ProjectUILocalize.actionRecentProjectsOpenSelectedProjectText());
            }
        }
        else {
            presentation.setEnabledAndVisible(false);
        }
    }
}
