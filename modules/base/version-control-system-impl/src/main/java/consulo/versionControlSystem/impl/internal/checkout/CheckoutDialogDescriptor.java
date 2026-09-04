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

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.versionControlSystem.checkout.CheckoutCallback;
import consulo.versionControlSystem.checkout.CheckoutPage;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
public class CheckoutDialogDescriptor extends DialogDescriptor {
    private final Project myProject;
    private final CheckoutProvider myProvider;
    private final CheckoutCallback myListener;

    private boolean myCheckoutEnabled;
    private @Nullable CheckoutPage myPage;

    @RequiredUIAccess
    public CheckoutDialogDescriptor(Project project, CheckoutProvider provider) {
        super(provider.getName().map(Presentation.NO_MNEMONIC));
        myProject = project;
        myProvider = provider;
        myListener = ProjectLevelVcsManagerEx.getInstanceEx(project).getCompositeCheckoutCallback(UIAccess.current(), false);
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        CheckoutPage page = myProvider.createPage(myProject, uiDisposable);
        myPage = page;

        return page.createComponent(enabled -> {
            myCheckoutEnabled = enabled;

            updateOkButtonState();
        });
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myCheckoutEnabled;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(myProvider.getActionName());
    }

    @Override
    public void onHandleValue(AnAction action, @Nullable DialogValue value) {
        if (myPage != null) {
            myPage.doCheckout(myListener);
        }
    }
}
