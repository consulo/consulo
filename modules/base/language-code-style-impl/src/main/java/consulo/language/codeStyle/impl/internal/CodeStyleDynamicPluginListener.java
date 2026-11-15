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
package consulo.language.codeStyle.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.ReadAction;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.project.Project;
import consulo.project.ProjectManager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Code style settings of a plugin loaded or unloaded at runtime are added to or removed from every settings instance.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class CodeStyleDynamicPluginListener implements DynamicPluginListener {
    @Override
    public void beforePluginUnload(PluginDescriptor pluginDescriptor) {
        ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();
        forEachSettings(settings -> settings.removePluginSettings(classLoader));
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        if (!change.getLoading().isEmpty()) {
            forEachSettings(CodeStyleSettings::addPluginSettings);
        }

        // after the file types of unloaded plugins are gone, so listeners do not pick up their files again
        if (!change.getLoading().isEmpty() || !change.getUnloading().isEmpty()) {
            notifySettingsChanged();
        }
    }

    private static void forEachSettings(Consumer<CodeStyleSettings> action) {
        Set<CodeStyleSettings> all = Collections.newSetFromMap(new IdentityHashMap<>());
        all.add(CodeStyleSettings.getDefaults());
        for (CodeStyleScheme scheme : CodeStyleSchemes.getInstance().getSchemes()) {
            all.add(scheme.getCodeStyleSettings());
        }

        collect(all, CodeStyleSettingsManager.getInstance());
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            collect(all, CodeStyleSettingsManager.getInstance(project));
        }

        all.forEach(action);
    }

    private static void notifySettingsChanged() {
        ReadAction.run(() -> {
            CodeStyleSettingsManager.getInstance().notifyCodeStyleSettingsChanged();
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                CodeStyleSettingsManager.getInstance(project).notifyCodeStyleSettingsChanged();
            }
        });
    }

    private static void collect(Set<CodeStyleSettings> all, CodeStyleSettingsManager manager) {
        CodeStyleSettings projectSettings = manager.PER_PROJECT_SETTINGS;
        if (projectSettings != null) {
            all.add(projectSettings);
        }
        CodeStyleSettings temporarySettings = manager.getTemporarySettings();
        if (temporarySettings != null) {
            all.add(temporarySettings);
        }
    }
}
