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
package consulo.configurable.internal;

import consulo.application.Application;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2016-11-05
 */
public class ConfigurableUIMigrationUtil {
    private static final Logger LOG = Logger.getInstance(ConfigurableUIMigrationUtil.class);

    @RequiredUIAccess
    public static @Nullable Component createUIComponent(UnnamedConfigurable configurable, Disposable parentUIDisposable) {
        Component uiComponent = createSupportedUIComponent(configurable, parentUIDisposable);
        if (uiComponent == null && Application.get().isUnifiedApplication()) {
            return createNotSupportedComponent();
        }
        return uiComponent;
    }

    @RequiredUIAccess
    public static @Nullable Component createSupportedUIComponent(UnnamedConfigurable configurable, Disposable parentUIDisposable) {
        try {
            return configurable.createUIComponent(parentUIDisposable);
        }
        catch (RuntimeException | LinkageError e) {
            if (!Application.get().isUnifiedApplication()) {
                throw e;
            }

            UnnamedConfigurable original = configurable instanceof ConfigurableWrapper wrapper ? wrapper.getConfigurable() : configurable;
            LOG.warn("Configurable " + original.getClass().getName() + " is not supported by this frontend", e);
            return null;
        }
    }

    @RequiredUIAccess
    public static Component createNotSupportedComponent() {
        return Label.create(ConfigurableLocalize.configurableNotSupportedHere());
    }
}
