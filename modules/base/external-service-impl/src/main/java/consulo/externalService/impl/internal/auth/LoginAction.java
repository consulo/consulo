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
package consulo.externalService.impl.internal.auth;

import consulo.application.Application;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.impl.internal.ExternalServiceConfigurationImpl;
import consulo.localize.LocalizeValue;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.RightAlignedToolbarAction;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2017-03-01
 */
public class LoginAction extends LegacyDumbAwareAction implements RightAlignedToolbarAction {
  private final Provider<ExternalServiceConfiguration> myExternalServiceConfigurationProvider;

  public LoginAction(Provider<ExternalServiceConfiguration> externalServiceConfigurationProvider) {
    super(LocalizeValue.localizeTODO("Login"), LocalizeValue.empty(), PlatformIconGroup.actionsLoginavatar());
    myExternalServiceConfigurationProvider = externalServiceConfigurationProvider;
  }

  @Override
  public void update(AnActionEvent e) {
    ExternalServiceConfiguration configuration = myExternalServiceConfigurationProvider.get();

    Presentation presentation = e.getPresentation();

    String email = configuration.getEmail();
    if (email == null) {
      presentation.setText("Not authorized…");
      presentation.setIcon(PlatformIconGroup.actionsLoginavatar());
    }
    else {
      presentation.setText(LocalizeValue.of(email));

      Image userIcon = configuration.getUserIcon();
      presentation.setIcon(ObjectUtil.notNull(userIcon, PlatformIconGroup.actionsLoginavatar()));
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    ExternalServiceConfiguration configuration = myExternalServiceConfigurationProvider.get();

    if(configuration.getEmail() != null) {
      MessageBoxes.yesNo().asWarning().text(LocalizeValue.localizeTODO("Do logout?")).showAsync().whenComplete((value, error) -> {
          if (error != null) {
              return;
          }

        if(value) {
          // call internal implementation
          ((ExternalServiceConfigurationImpl) configuration).reset();
        }
      });
    } else {
      UIAccess uiAccess = UIAccess.current();

      Application.get().getInstance(ExternalServiceLoginService.class).login().whenCompleteAsync((userName, error) -> {
        if (error != null) {
          MessageBoxes.okError(ExternalServiceLocalize.messageFailedToReceiveOauthToken()).showAsync();
        }
        else {
          MessageBoxes.okInfo(ExternalServiceLocalize.messageSuccessfullyLoggedInAs0(userName)).showAsync();
        }
      }, uiAccess);
    }
  }
}
