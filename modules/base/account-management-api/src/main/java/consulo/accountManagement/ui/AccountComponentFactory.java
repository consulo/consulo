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
package consulo.accountManagement.ui;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountType;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AccountComponentFactory {
    @RequiredUIAccess
    Avatar createAvatar(Account account, @Nullable AvatarStyle style, Disposable parentDisposable);

    @RequiredUIAccess
    AccountChooser createChooser(AccountType<?> type, @Nullable Project project, Disposable parentDisposable);
}
