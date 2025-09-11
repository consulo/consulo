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

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.SortedMap;
import java.util.TreeMap;

@ActionImpl(id = "Vcs.Checkout")
public class CheckoutActionGroup extends ActionGroup implements DumbAware {
    private final Application myApplication;

    @Inject
    public CheckoutActionGroup(Application application) {
        myApplication = application;
        setPopup(true);
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        if (!myApplication.getExtensionPoint(CheckoutProvider.class).hasAnyExtensions()) {
            e.getPresentation().setVisible(false);
        }
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        SortedMap<LocalizeValue, AnAction> actions = myApplication.getExtensionPoint(CheckoutProvider.class)
            .collectMapped(new TreeMap<>(), p -> p.getName().map(Presentation.NO_MNEMONIC), CheckoutAction::new);
        return actions.values().toArray(AnAction[]::new);
    }
}
