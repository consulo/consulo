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
package consulo.auth.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.ObjectUtil;
import consulo.auth.ServiceAuthConfiguration;
import consulo.auth.ServiceAuthEarlyAccessProgramDescriptor;
import consulo.auth.ui.ServiceAuthDialog;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Mar-17
 */
public class LoginAction extends AnAction implements RightAlignedToolbarAction, DumbAware {
  public LoginAction() {
    super("Login", null, AllIcons.Actions.LoginAvator);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (!EarlyAccessProgramManager.is(ServiceAuthEarlyAccessProgramDescriptor.class)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    ServiceAuthConfiguration configuration = ServiceAuthConfiguration.getInstance();

    Presentation presentation = e.getPresentation();

    String email = configuration.getEmail();
    if (email == null) {
      presentation.setText("Logged as anonymous");
      presentation.setIcon(AllIcons.Actions.LoginAvator);
    }
    else {
      presentation.setText("Logged as '" + email + "'");

      Image userIcon = configuration.getUserIcon();
      presentation.setIcon(ObjectUtil.notNull(userIcon, AllIcons.Actions.LoginAvator));
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ServiceAuthDialog dialog = new ServiceAuthDialog();
    dialog.show();
  }
}
