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

import consulo.ide.impl.idea.openapi.vcs.checkout.CheckoutAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WelcomeGetFromVcsAction extends WelcomePopupAction {

    @Override
    protected void fillActions(DefaultActionGroup group) {
        final List<CheckoutProvider> providers = new ArrayList<>(CheckoutProvider.EXTENSION_POINT_NAME.getExtensionList());
        providers.sort(new CheckoutProvider.CheckoutProviderComparator());
        for (CheckoutProvider provider : providers) {
            group.add(new CheckoutAction(provider));
        }
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.welcomeFromvcs();
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

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(CheckoutProvider.EXTENSION_POINT_NAME.hasAnyExtensions());
        e.getPresentation().setIcon(PlatformIconGroup.welcomeFromvcs());
    }
}
