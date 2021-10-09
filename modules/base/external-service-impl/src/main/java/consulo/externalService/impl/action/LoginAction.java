/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalService.impl.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.ObjectUtil;
import consulo.externalService.impl.HubAuthorizationService;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Mar-17
 */
public class LoginAction extends AnAction implements RightAlignedToolbarAction, DumbAware {
  private final Provider<HubAuthorizationService> myHubAuthorizationServiceProvider;

  public LoginAction(Provider<HubAuthorizationService> hubAuthorizationServiceProvider) {
    super(LocalizeValue.localizeTODO("Login"), LocalizeValue.empty(), AllIcons.Actions.LoginAvator);
    myHubAuthorizationServiceProvider = hubAuthorizationServiceProvider;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    HubAuthorizationService hubAuthorizationService = myHubAuthorizationServiceProvider.get();

    Presentation presentation = e.getPresentation();

    String email = hubAuthorizationService.getEmail();
    if (email == null) {
      presentation.setTextValue(LocalizeValue.localizeTODO("Not authorized..."));
      presentation.setIcon(AllIcons.Actions.LoginAvator);
    }
    else {
      presentation.setTextValue(LocalizeValue.of(email));

      Image userIcon = hubAuthorizationService.getUserIcon();
      presentation.setIcon(ObjectUtil.notNull(userIcon, AllIcons.Actions.LoginAvator));
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    HubAuthorizationService hubAuthorizationService = myHubAuthorizationServiceProvider.get();

    UIAccess uiAccess = UIAccess.current();
    if (hubAuthorizationService.getEmail() != null) {
      Alerts.yesNo().asWarning().text(LocalizeValue.localizeTODO("Do logout?")).showAsync().doWhenDone(value -> {
        if (value) {
          hubAuthorizationService.reset();
        }
      });
    }
    else {
      AsyncResult<Void> result = myHubAuthorizationServiceProvider.get().openLinkSite(false);
      result.doWhenDone(() -> {
        uiAccess.give(() -> Alerts.okInfo(LocalizeValue.localizeTODO("Successfully logged as " + hubAuthorizationService.getEmail())).showAsync());
      });
      result.doWhenRejected(() -> {
        uiAccess.give(() -> Alerts.okError(LocalizeValue.localizeTODO("Failed to request oauth token")).showAsync());
      });
    }
  }
}
