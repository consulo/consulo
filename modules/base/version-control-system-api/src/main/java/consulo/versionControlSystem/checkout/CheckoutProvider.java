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
package consulo.versionControlSystem.checkout;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * Implement this interface and register it as extension to checkoutProvider extension point in order to provide checkout
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CheckoutProvider {
    @Deprecated
    default String getVcsName() {
        return getClass().getSimpleName();
    }

    default LocalizeValue getName() {
        return LocalizeValue.localizeTODO(TextWithMnemonic.parse(getVcsName()).getText());
    }

    default Image getIcon() {
        return VersionControlSystemIconGroup.branch();
    }

    default LocalizeValue getActionName() {
        return VcsLocalize.checkoutButtonText();
    }

    @RequiredUIAccess
    CheckoutPage createPage(Project project, Disposable uiDisposable);
}
