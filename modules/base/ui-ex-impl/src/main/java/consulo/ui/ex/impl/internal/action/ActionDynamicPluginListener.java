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
package consulo.ui.ex.impl.internal.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.ex.action.ActionManager;
import jakarta.inject.Inject;

/**
 * Actions of a plugin loaded at runtime join an action manager which is created already - a later created one reads them
 * with the other actions. Actions of an unloaded plugin leave it.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class ActionDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;

    @Inject
    public ActionDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public void pluginLoaded(PluginDescriptor pluginDescriptor) {
        if (myApplication.getInstanceIfCreated(ActionManager.class) instanceof ActionManagerImpl actionManager) {
            actionManager.loadPluginActions(pluginDescriptor);
        }
    }

    @Override
    public void beforePluginUnload(PluginDescriptor pluginDescriptor) {
        if (myApplication.getInstanceIfCreated(ActionManager.class) instanceof ActionManagerImpl actionManager) {
            actionManager.unloadPluginActions(pluginDescriptor);
        }
    }
}
