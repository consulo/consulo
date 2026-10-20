/*
 * Copyright 2013-2026 consulo.io
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
package consulo.accountManagement.impl.internal.ui;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Avatar;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.RenderItem;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
final class AccountRow {
    static final Key<AccountRow> KEY = Key.create(AccountRow.class);

    private final AccountManager myManager;
    private final DockLayout myRoot;
    private final Avatar myAvatar;
    private final Label myName;
    private final Label myDetail;
    private final Badge myActive;

    @RequiredUIAccess
    AccountRow(AccountManager manager) {
        myManager = manager;

        myAvatar = Avatar.create(LocalizeValue.empty());

        myName = Label.create();
        myDetail = Label.create();
        myDetail.setForegroundColor(ComponentColors.DISABLED_TEXT);
        myDetail.addStyle(LabelStyle.FONT_SMALL);

        myActive = Badge.create(AccountManagementLocalize.labelCurrentlyActive());
        myActive.addStyle(BadgeStyle.SMALL);

        VerticalLayout text = VerticalLayout.create(Space.X_SMALL);
        text.add(myName);
        text.add(myDetail);
        text.add(HorizontalLayout.create().add(myActive));

        HorizontalLayout left = HorizontalLayout.create(Space.MEDIUM);
        left.add(myAvatar);
        left.add(text);

        myRoot = DockLayout.create(Space.MEDIUM);
        myRoot.left(left);
        myRoot.paddingBuilder().verticalSet(Space.MEDIUM).horizontalSet(Space.LARGE).apply();
        myRoot.putUserData(KEY, this);
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    void bind(RenderItem<Object> item) {
        if (!(item.getValue() instanceof Account account)) {
            return;
        }
        AccountType<?> type = myManager.findAccountType(account.getTypeId());

        String login = account.getLogin();
        myAvatar.setName(login == null && account.getAvatarUrl() == null ? LocalizeValue.empty() : LocalizeValue.of(account.getName()));
        Image avatar = myManager.getAvatar(account).get();
        myAvatar.setImage(avatar != null ? avatar : login != null || type == null ? null : type.getIcon());

        myName.setText(LocalizeValue.of(account.getName()));
        if (account.isAuthFailed()) {
            myDetail.setText(AccountManagementLocalize.labelReloginNeeded());
            myDetail.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        }
        else {
            String detail = login != null ? login : account.getServerUrl();
            myDetail.setText(detail == null ? LocalizeValue.empty() : LocalizeValue.of(detail));
            myDetail.setForegroundColor(ComponentColors.DISABLED_TEXT);
        }

        myActive.setVisible(type != null && myManager.getActiveAccount(type, null) == account);
    }
}
