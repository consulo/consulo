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
package consulo.execution.profiler.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.execution.profiler.ProfilerConfigurableIds;
import consulo.execution.profiler.configuration.EditProfilerConfigurationComponent;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.LabeledBuilder;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class ProfilerGroupConfigurable extends EditProfilerConfigurationComponent
    implements ApplicationConfigurable, Configurable.NoScroll {
    private final Application myApplication;
    private final ProfilerUISettings myUISettings;

    private @Nullable ComboBox<ProfilerSessionHost> mySessionHostBox;

    @Inject
    public ProfilerGroupConfigurable(Application application, ProfilerUISettings uiSettings) {
        super(application, ProfilerConfigurableIds.GROUP, null);
        myApplication = application;
        myUISettings = uiSettings;
    }

    @Override
    public String getId() {
        return ProfilerConfigurableIds.GROUP;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Profilers");
    }

    @Override
    @RequiredUIAccess
    public Component createUIComponent(Disposable uiDisposable) {
        ComboBox.Builder<ProfilerSessionHost> builder = ComboBox.builder();
        for (ProfilerSessionHost host : ProfilerSessionHost.values()) {
            builder.add(host, host.getDisplayName());
        }
        ComboBox<ProfilerSessionHost> sessionHostBox = builder.build();
        mySessionHostBox = sessionHostBox;

        DockLayout layout = DockLayout.create();
        layout.top(LabeledBuilder.sided(LocalizeValue.localizeTODO("Open profiling sessions in:"), sessionHostBox));
        layout.center(createConfigurationsComponent(uiDisposable));
        return layout;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return super.isModified() || getSelectedSessionHost() != myUISettings.getSessionHost();
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        super.apply();
        myUISettings.setSessionHost(getSelectedSessionHost());
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        super.reset();
        ComboBox<ProfilerSessionHost> sessionHostBox = mySessionHostBox;
        if (sessionHostBox != null) {
            sessionHostBox.setValue(myUISettings.getSessionHost());
        }
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        super.disposeUIResources();
        mySessionHostBox = null;
    }

    private ProfilerSessionHost getSelectedSessionHost() {
        ComboBox<ProfilerSessionHost> sessionHostBox = mySessionHostBox;
        ProfilerSessionHost host = sessionHostBox == null ? null : sessionHostBox.getValue();
        return host == null ? myUISettings.getSessionHost() : host;
    }

    @RequiredUIAccess
    private Component createConfigurationsComponent(Disposable uiDisposable) {
        AtomicBoolean anyType = new AtomicBoolean();
        AtomicBoolean groupType = new AtomicBoolean();
        myApplication.getExtensionPoint(ProfilerConfigurationTypeBase.class).forEach(type -> {
            anyType.set(true);
            if (ProfilerConfigurableIds.GROUP.equals(type.getLanguageSettingsGroup())) {
                groupType.set(true);
            }
        });

        if (groupType.get()) {
            return super.createUIComponent(uiDisposable);
        }

        Label label = Label.create(
            anyType.get()
                ? LocalizeValue.localizeTODO("Profiler configurations are edited on the pages of their profilers.")
                : LocalizeValue.localizeTODO("No profilers are installed.")
        );
        label.setForegroundColor(ComponentColors.INFO_FOREGROUND);

        VerticalLayout layout = VerticalLayout.create();
        layout.add(label);
        return layout;
    }
}
