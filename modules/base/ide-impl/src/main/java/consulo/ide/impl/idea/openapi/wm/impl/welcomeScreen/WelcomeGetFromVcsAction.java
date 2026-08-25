/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.vcs.checkout.UnifiedCheckoutPanel;
import consulo.ide.impl.welcomeScreen.WelcomeSlide;
import consulo.ide.impl.welcomeScreen.WelcomeSlideAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.inject.Inject;

@ActionImpl(id = "WelcomeScreen.GetFromVcs")
public class WelcomeGetFromVcsAction extends WelcomeSlideAction implements AnActionWithSyncUpdate {
    private final Application myApplication;

    @Inject
    public WelcomeGetFromVcsAction(Application application) {
        super(
            ActionLocalize.actionWelcomescreenGetfromvcsText(),
            ActionLocalize.actionWelcomescreenGetfromvcsDescription(),
            PlatformIconGroup.welcomeFromvcs()
        );
        myApplication = application;
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    protected LocalizeValue getSlideTitle() {
        return VcsLocalize.checkoutTitle();
    }

    @Override
    @RequiredUIAccess
    protected WelcomeSlide createSlide(Disposable parentDisposable, TitlelessDecorator titlelessDecorator) {
        return new UnifiedCheckoutPanel(myApplication, ProjectManager.getInstance().getDefaultProject(), titlelessDecorator);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(myApplication.getExtensionPoint(CheckoutProvider.class).hasAnyExtensions());
        e.getPresentation().setIcon(PlatformIconGroup.welcomeFromvcs());
    }
}
