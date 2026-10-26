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
package consulo.versionControlSystem.impl.internal.checkout;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.versionControlSystem.checkout.CheckoutDialogService;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class CheckoutDialogServiceImpl implements CheckoutDialogService {
    private final DialogService myDialogService;

    @Inject
    public CheckoutDialogServiceImpl(DialogService dialogService) {
        myDialogService = dialogService;
    }

    @RequiredUIAccess
    @Override
    public void showCheckoutDialog(Project project, CheckoutProvider provider) {
        myDialogService.build(project, new CheckoutDialogDescriptor(project, provider)).showAsync();
    }
}
