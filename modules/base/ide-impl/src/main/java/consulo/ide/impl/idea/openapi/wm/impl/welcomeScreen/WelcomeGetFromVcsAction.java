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
import consulo.ide.impl.idea.openapi.vcs.checkout.CheckoutAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.localize.UILocalize;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.*;

@ActionImpl(id = "WelcomeScreen.GetFromVcs")
public class WelcomeGetFromVcsAction extends WelcomePopupAction {
    @Nonnull
    private final Application myApplication;

    @Inject
    public WelcomeGetFromVcsAction(@Nonnull Application application) {
        super(
            ActionLocalize.actionWelcomescreenGetfromvcsText(),
            ActionLocalize.actionWelcomescreenGetfromvcsDescription(),
            PlatformIconGroup.welcomeFromvcs()
        );
        myApplication = application;
    }

    @Override
    protected void fillActions(DefaultActionGroup group) {
        SortedMap<LocalizeValue, AnAction> actions = myApplication.getExtensionPoint(CheckoutProvider.class)
            .collectMapped(new TreeMap<>(), p -> p.getName().map(Presentation.NO_MNEMONIC), CheckoutAction::new);
        group.addAll(actions.values());
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Nonnull
    @Override
    protected LocalizeValue getTextForEmpty() {
        return UILocalize.welcomeScreenGetFromVcsActionNoVcsPluginsWithCheckOutActionInstalledActionName();
    }

    @Override
    protected boolean isSilentlyChooseSingleOption() {
        return true;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(myApplication.getExtensionPoint(CheckoutProvider.class).hasAnyExtensions());
        e.getPresentation().setIcon(PlatformIconGroup.welcomeFromvcs());
    }
}
