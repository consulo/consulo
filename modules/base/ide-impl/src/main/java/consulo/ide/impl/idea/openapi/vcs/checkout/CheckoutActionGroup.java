/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class CheckoutActionGroup extends ActionGroup implements DumbAware {
    private AnAction[] myChildren;

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        if (!CheckoutProvider.EXTENSION_POINT_NAME.hasAnyExtensions()) {
            e.getPresentation().setVisible(false);
        }
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (myChildren == null) {
            List<CheckoutProvider> extensionList = CheckoutProvider.EXTENSION_POINT_NAME.getExtensionList();
            myChildren = extensionList.stream()
                .sorted(new CheckoutProvider.CheckoutProviderComparator())
                .map(CheckoutAction::new)
                .toArray(AnAction[]::new);
        }
        return myChildren;
    }
}
