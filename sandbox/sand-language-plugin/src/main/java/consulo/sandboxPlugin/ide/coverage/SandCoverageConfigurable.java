/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ide.coverage;

import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

/**
 * @author VISTALL
 * @since 2020-09-20
 */
public class SandCoverageConfigurable extends SimpleConfigurableByProperties implements Configurable {
    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(CheckBox.create(LocalizeValue.localizeTODO("Some sand option")));
        return layout;
    }

    @Nls
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Sand options");
    }
}
