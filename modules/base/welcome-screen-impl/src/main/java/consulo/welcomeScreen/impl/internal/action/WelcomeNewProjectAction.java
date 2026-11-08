/*
 * Copyright 2013-2024 consulo.io
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
package consulo.welcomeScreen.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.disposer.Disposable;
import consulo.ide.impl.newProject.actions.NewProjectAction;
import consulo.welcomeScreen.WelcomeSlide;
import consulo.welcomeScreen.WelcomeSlideAction;
import consulo.localize.LocalizeValue;
import consulo.module.creation.impl.internal.UnifiedNewProjectPanel;
import consulo.module.creation.impl.localize.ModuleCreationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.IdeActions;
import consulo.welcomeScreen.impl.internal.NewProjectWelcomeSlide;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
@ActionImpl(
    id = "WelcomeScreen.CreateNewProject",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART), anchor = ActionRefAnchor.FIRST)
)
public class WelcomeNewProjectAction extends WelcomeSlideAction {
    public WelcomeNewProjectAction() {
        super(
            ActionLocalize.actionWelcomescreenCreatenewprojectText(),
            ActionLocalize.actionWelcomescreenCreatenewprojectDescription(),
            PlatformIconGroup.welcomeCreatenewproject()
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    protected LocalizeValue getSlideTitle() {
        return ModuleCreationImplLocalize.titleNewProject();
    }

    @Override
    @RequiredUIAccess
    protected WelcomeSlide createSlide(Disposable parentDisposable, TitlelessDecorator titlelessDecorator) {
        UnifiedNewProjectPanel panel = new UnifiedNewProjectPanel(parentDisposable, null, titlelessDecorator);
        panel.setDefaultOkAction(() -> NewProjectAction.generateProject(null, panel));
        return new NewProjectWelcomeSlide(panel);
    }
}
