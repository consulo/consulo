/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.idea.ide.actions.RevealFileAction;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.platform.Platform;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2021-05-03
 */
@ActionImpl(id = "WelcomeScreen.RevealRecentProjectDirectory")
public class RevealRecentProjectDirectoryAction extends RecentProjectsWelcomeScreenActionBase {
    public RevealRecentProjectDirectoryAction() {
        super(RevealFileAction.getActionName(null));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setTextValue(RevealFileAction.getActionName(e.getPlace()));

        List<AnAction> elements = getSelectedElements(e);
        boolean enable = elements.size() == 1 && elements.get(0) instanceof ReopenProjectAction;
        presentation.setEnabledAndVisible(enable);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        List<AnAction> elements = getSelectedElements(e);
        if (elements.size() != 1) {
            return;
        }

        AnAction action = elements.get(0);
        if (action instanceof ReopenProjectAction reopenProjectAction) {
            String projectPath = reopenProjectAction.getProjectPath();

            Platform.current().openDirectoryInFileManager(new File(projectPath), UIAccess.current());
        }
    }
}
