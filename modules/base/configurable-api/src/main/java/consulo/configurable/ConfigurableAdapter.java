/*
 * Copyright 2013-2016 consulo.io
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
package consulo.configurable;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 06.01.2016
 */
public class ConfigurableAdapter implements Configurable {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.empty();
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public JComponent createComponent(@Nonnull Disposable parentDisposable) {
        return null;
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createUIComponent(@Nonnull Disposable parentDisposable) {
        return null;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
    }

    @RequiredUIAccess
    @Override
    public void reset() {
    }
}
