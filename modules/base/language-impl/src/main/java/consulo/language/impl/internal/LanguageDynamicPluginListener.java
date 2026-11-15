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
package consulo.language.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.language.ast.IElementType;
import consulo.language.impl.internal.sem.SemServiceImpl;
import consulo.language.internal.LanguageRegistryImpl;
import consulo.language.psi.ElementTypeEntryExtensionCollector;
import consulo.language.sem.SemService;
import consulo.project.Project;
import consulo.project.ProjectManager;

/**
 * Languages and element types of a plugin unloaded at runtime leave the registries.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class LanguageDynamicPluginListener implements DynamicPluginListener {
    @Override
    public void pluginUnloaded(PluginDescriptor pluginDescriptor) {
        ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();

        IElementType.unregisterTypes(classLoader);
        LanguageRegistryImpl.unregisterLanguages(classLoader);
        ElementTypeEntryExtensionCollector.clearAll();

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && project.getInstanceIfCreated(SemService.class) instanceof SemServiceImpl semService) {
                semService.dropProducers();
            }
        }
    }
}
