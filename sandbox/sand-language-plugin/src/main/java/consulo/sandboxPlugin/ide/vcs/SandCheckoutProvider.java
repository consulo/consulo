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
package consulo.sandboxPlugin.ide.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.versionControlSystem.checkout.CheckoutPage;
import consulo.versionControlSystem.checkout.CheckoutProvider;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
@ExtensionImpl
public class SandCheckoutProvider implements CheckoutProvider {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Sand VCS");
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesStatic();
    }

    @Override
    public LocalizeValue getActionName() {
        return LocalizeValue.localizeTODO("Clone");
    }

    @Override
    @RequiredUIAccess
    public CheckoutPage createPage(Project project, Disposable uiDisposable) {
        return new SandCheckoutPage(project, uiDisposable);
    }
}
