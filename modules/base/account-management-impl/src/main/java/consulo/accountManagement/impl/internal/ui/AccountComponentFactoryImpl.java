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
import consulo.accountManagement.ui.AccountChooser;
import consulo.accountManagement.ui.AccountComponentFactory;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@Singleton
@ServiceImpl
public class AccountComponentFactoryImpl implements AccountComponentFactory {
    private final AccountManager myManager;

    @Inject
    public AccountComponentFactoryImpl(AccountManager manager) {
        myManager = manager;
    }

    @RequiredUIAccess
    @Override
    public Avatar createAvatar(Account account, @Nullable AvatarStyle style, Disposable parentDisposable) {
        Avatar avatar = Avatar.create(account.getLogin() == null && account.getAvatarUrl() == null ? LocalizeValue.empty() : LocalizeValue.of(account.getName()));
        if (style != null) {
            avatar.addStyle(style);
        }
        AccountType<?> type = myManager.findAccountType(account.getTypeId());
        Image typeIcon = type == null ? null : type.getIcon();
        boolean hasName = account.getLogin() != null;

        Image loaded = myManager.getAvatar(account).get();
        avatar.setImage(loaded != null ? loaded : hasName ? null : typeIcon);

        UIAccess uiAccess = UIAccess.current();
        Runnable unsubscribe = myManager.getAvatar(account).addListener(image -> uiAccess.execute(() -> {
            if (image != null) {
                avatar.setImage(image);
            }
        }));
        Disposer.register(parentDisposable, unsubscribe::run);
        return avatar;
    }

    @RequiredUIAccess
    @Override
    public AccountChooser createChooser(AccountType<?> type, @Nullable Project project, Disposable parentDisposable) {
        return new AccountChooserImpl(myManager, type, project, parentDisposable);
    }
}
