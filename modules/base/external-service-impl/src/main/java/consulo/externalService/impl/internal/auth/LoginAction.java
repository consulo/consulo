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

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.RightAlignedToolbarAction;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2017-03-01
 */
public class LoginAction extends DumbAwareAction implements RightAlignedToolbarAction, AnActionWithSyncUpdate {
    private final Provider<HubAccounts> myHubAccounts;
    private final Provider<AccountManager> myAccountManager;

    public LoginAction(Provider<HubAccounts> hubAccounts, Provider<AccountManager> accountManager) {
        super(ExternalServiceLocalize.hubActionLogIn(), LocalizeValue.empty(), PlatformIconGroup.actionsLoginavatar());
        myHubAccounts = hubAccounts;
        myAccountManager = accountManager;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Account account = myHubAccounts.get().getActiveAccount();
        if (account == null) {
            presentation.setText(ExternalServiceLocalize.hubActionLogIn());
            presentation.setIcon(PlatformIconGroup.actionsLoginavatar());
            return;
        }
        presentation.setText(LocalizeValue.of(account.getName()));
        Image avatar = myAccountManager.get().getAvatar(account).get();
        presentation.setIcon(avatar != null ? ImageEffects.resize(avatar, Image.DEFAULT_ICON_SIZE) : PlatformIconGroup.actionsLoginavatar());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        HubAccounts hubAccounts = myHubAccounts.get();
        Account account = hubAccounts.getActiveAccount();
        if (account != null) {
            String userName = account.getName();
            MessageBoxes.yesNo()
                .asWarning()
                .text(ExternalServiceLocalize.hubLogOutQuestion(userName))
                .showAsync()
                .whenComplete((value, error) -> {
                    if (error == null && Boolean.TRUE.equals(value)) {
                        hubAccounts.logout().whenComplete((ignored, logoutError) -> HubNotifications.loggedOut(userName));
                    }
                });
            return;
        }
        hubAccounts.login().whenCompleteAsync(HubNotifications::loggedIn, UIAccess.current());
    }
}
