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
package consulo.execution.profiler.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.execution.profiler.DefaultProfilerExecutorGroup;
import consulo.execution.profiler.ProfilerExecutorSettings;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.execution.profiler.configuration.ProfilerRunConfigurationManager;
import consulo.execution.profiler.configuration.ProfilerStarter;
import consulo.execution.profiler.icon.ExecutionProfilerIconGroup;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "profiler", order = "after coverage")
public class DefaultProfilerExecutorGroupImpl extends DefaultProfilerExecutorGroup {
    public static final String EXECUTOR_ID = "Profiler.ExecutorGroup";
    public static final String CONTEXT_ACTION_ID = "Profiler.RunContextGroup";

    private final Application myApplication;
    private final ProfilerRunConfigurationManager myManager;
    private final List<ProfilerExecutorSettingsImpl> myRegisteredSettings = new ArrayList<>();

    @Inject
    public DefaultProfilerExecutorGroupImpl(Application application, ProfilerRunConfigurationManager manager) {
        myApplication = application;
        myManager = manager;
        manager.addChangeListener(this::syncSettings);
        syncSettings();
    }

    @Override
    public String getId() {
        return EXECUTOR_ID;
    }

    @Override
    public String getContextActionId() {
        return CONTEXT_ACTION_ID;
    }

    @Override
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public Image getToolWindowIcon() {
        return PlatformIconGroup.toolwindowsToolwindowrun();
    }

    @Override
    public Image getToolWindowIconIfRunning() {
        return PlatformIconGroup.toolwindowsToolwindowrunactive();
    }

    @Override
    public Image getIcon() {
        return ExecutionProfilerIconGroup.profile();
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Profile the selected configuration");
    }

    @Override
    public LocalizeValue getActionName() {
        return LocalizeValue.localizeTODO("Profile");
    }

    @Override
    public LocalizeValue getStartActionText() {
        return LocalizeValue.localizeTODO("Profile with");
    }

    @Override
    public LocalizeValue getStartActiveText(String configurationName) {
        if (configurationName.isEmpty()) {
            return getStartActionText();
        }
        return LocalizeValue.localizeTODO("Profile '" + configurationName + "' with");
    }

    @Override
    public LocalizeValue getRunToolbarActionText(String param) {
        return LocalizeValue.localizeTODO("Profile with '" + param + "'");
    }

    @Override
    public LocalizeValue getRunToolbarChooserText() {
        return LocalizeValue.localizeTODO("Profile with…");
    }

    @Override
    public boolean isApplicable(Project project) {
        for (Pair<String, ProfilerExecutorSettings> settings : allRegisteredSettings()) {
            if (settings.getSecond().isApplicable(project)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void syncSettings() {
        Map<String, ProfilerConfigurationTypeBase<?>> types = new LinkedHashMap<>();
        myApplication.getExtensionPoint(ProfilerConfigurationTypeBase.class).forEach(type -> types.putIfAbsent(type.getId(), type));

        List<ProfilerExecutorSettingsImpl> desired = new ArrayList<>();
        for (ProfilerConfigurationState state : myManager.getConfigurations()) {
            ProfilerConfigurationTypeBase<?> type = types.get(state.getConfigurationTypeId());
            if (type == null) {
                continue;
            }

            ProfilerStarter starter = ProfilerExecutorSettingsImpl.createStarter(type, state);
            if (starter != null) {
                desired.add(new ProfilerExecutorSettingsImpl(type, state, starter));
            }
        }

        int kept = 0;
        while (kept < desired.size() && kept < myRegisteredSettings.size()) {
            ProfilerExecutorSettingsImpl registered = myRegisteredSettings.get(kept);
            ProfilerExecutorSettingsImpl wanted = desired.get(kept);
            if (!registered.isSameConfiguration(wanted.getType(), wanted.getState())) {
                break;
            }
            registered.update(wanted.getState(), wanted.getStarter());
            kept++;
        }

        for (int i = myRegisteredSettings.size() - 1; i >= kept; i--) {
            unregisterSettings(myRegisteredSettings.remove(i));
        }

        for (int i = kept; i < desired.size(); i++) {
            ProfilerExecutorSettingsImpl settings = desired.get(i);
            registerSettings(settings);
            myRegisteredSettings.add(settings);
        }
    }
}
