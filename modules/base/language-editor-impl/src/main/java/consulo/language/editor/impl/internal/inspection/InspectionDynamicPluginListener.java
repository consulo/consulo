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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Inspection tools of a plugin loaded or unloaded at runtime join or leave every initialized profile. The profiles are
 * announced and the daemons restarted once per session.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class InspectionDynamicPluginListener implements DynamicPluginListener {
    @Override
    public void beforePluginUnload(PluginDescriptor pluginDescriptor) {
        ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();
        forEachProfile((profile, project) -> profile.removePluginTools(classLoader));
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        if (!change.getLoading().isEmpty()) {
            forEachProfile(InspectionProfileImpl::addPluginTools);
        }

        if (change.getLoading().isEmpty() && change.getUnloading().isEmpty()) {
            return;
        }

        InspectionProfileManager applicationManager = InspectionProfileManager.getInstance();
        for (Profile profile : applicationManager.getProfiles()) {
            if (profile instanceof InspectionProfileImpl profileImpl) {
                applicationManager.fireProfileChanged(profileImpl);
            }
        }

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                DaemonCodeAnalyzer.getInstance(project).restart();
            }
        }
    }

    private static void forEachProfile(BiConsumer<InspectionProfileImpl, @Nullable Project> action) {
        for (Profile profile : InspectionProfileManager.getInstance().getProfiles()) {
            if (profile instanceof InspectionProfileImpl profileImpl) {
                action.accept(profileImpl, null);
            }
        }

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }

            for (Profile profile : InspectionProjectProfileManager.getInstance(project).getProfiles()) {
                if (profile instanceof InspectionProfileImpl profileImpl) {
                    action.accept(profileImpl, project);
                }
            }
        }
    }
}
