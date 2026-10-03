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
package consulo.sandboxPlugin.ide.profiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.configurable.UnnamedConfigurable;
import consulo.execution.profiler.configuration.ProfilerAttacher;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.execution.profiler.configuration.ProfilerStarter;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandProfilerConfigurationType extends ProfilerConfigurationTypeBase<SandProfilerConfigurationState> {
    public static final String ID = "sand.profiler";
    public static final String LANGUAGE_SETTINGS_GROUP = "profiler.sand";

    private static final LocalizeValue DISPLAY_NAME = LocalizeValue.localizeTODO("Sand Profiler");

    private final ApplicationConcurrency myConcurrency;

    @Inject
    public SandProfilerConfigurationType(ApplicationConcurrency concurrency) {
        myConcurrency = concurrency;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsProfilecpu();
    }

    @Override
    public String getLanguageSettingsGroup() {
        return LANGUAGE_SETTINGS_GROUP;
    }

    @Override
    public SandProfilerConfigurationState getTemplateState() {
        SandProfilerConfigurationState state = new SandProfilerConfigurationState();
        state.setDisplayName(DISPLAY_NAME.get());
        return state;
    }

    @Override
    public UnnamedConfigurable createConfigurable(SandProfilerConfigurationState state) {
        return new SandProfilerConfigurable(state);
    }

    @Override
    public ProfilerStarter createStarter(SandProfilerConfigurationState state) {
        return new SandProfilerStarter();
    }

    @Override
    public ProfilerAttacher createAttacher(SandProfilerConfigurationState state) {
        return new SandProfilerAttacher(myConcurrency, state);
    }
}
