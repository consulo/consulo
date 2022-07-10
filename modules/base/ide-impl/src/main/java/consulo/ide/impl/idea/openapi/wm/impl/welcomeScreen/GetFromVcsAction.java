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

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.vcs.CheckoutProvider;
import consulo.ide.impl.idea.openapi.vcs.checkout.CheckoutAction;
import consulo.ui.ex.UIBundle;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GetFromVcsAction extends WelcomePopupAction{

  @Override
  protected void fillActions(DefaultActionGroup group) {
    final List<CheckoutProvider> providers = new ArrayList<>(CheckoutProvider.EXTENSION_POINT_NAME.getExtensionList());
    ContainerUtil.sort(providers, new CheckoutProvider.CheckoutProviderComparator());
    for (CheckoutProvider provider : providers) {
      group.add(new CheckoutAction(provider));
    }
  }

  @Override
  protected String getCaption() {
    return null;
  }

  @Override
  protected String getTextForEmpty() {
    return UIBundle.message("welcome.screen.get.from.vcs.action.no.vcs.plugins.with.check.out.action.installed.action.name");
  }

  @Override
  protected boolean isSilentlyChooseSingleOption() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(CheckoutProvider.EXTENSION_POINT_NAME.hasAnyExtensions());
    if (WelcomeFrameManager.isFromWelcomeFrame(e)) {
      e.getPresentation().setIcon(AllIcons.Welcome.FromVCS);
    }
  }
}
