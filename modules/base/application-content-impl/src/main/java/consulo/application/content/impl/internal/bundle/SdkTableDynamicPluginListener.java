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
package consulo.application.content.impl.internal.bundle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.content.bundle.SdkTable;
import jakarta.inject.Inject;

/**
 * SDKs of a type from a plugin loaded or unloaded at runtime switch to or from the unknown type.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@TopicImpl(ComponentScope.APPLICATION)
public class SdkTableDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;

    @Inject
    public SdkTableDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public void beforePluginUnload(PluginDescriptor pluginDescriptor) {
        ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();
        myApplication.runWriteAction(() -> getSdkTable().beforePluginUnload(classLoader));
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        myApplication.runWriteAction(() -> getSdkTable().sdkTypesChanged());
    }

    private SdkTableImpl getSdkTable() {
        return (SdkTableImpl) myApplication.getInstance(SdkTable.class);
    }
}
