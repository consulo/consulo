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
package consulo.language.inject.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.language.inject.InjectedLanguageManager;
import consulo.project.Project;
import jakarta.inject.Inject;

/**
 * The injector map holds injectors and element classes of plugins, it is dropped right away when plugins are unloaded,
 * not on the next injection.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicImpl(ComponentScope.PROJECT)
public class InjectionCacheDynamicPluginListener implements DynamicPluginListener {
    private final Project myProject;

    @Inject
    public InjectionCacheDynamicPluginListener(Project project) {
        myProject = project;
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        if (!change.getUnloading().isEmpty()
            && myProject.getInstanceIfCreated(InjectedLanguageManager.class) instanceof InjectedLanguageManagerImpl manager) {
            manager.clearInjectorCache();
        }
    }
}
