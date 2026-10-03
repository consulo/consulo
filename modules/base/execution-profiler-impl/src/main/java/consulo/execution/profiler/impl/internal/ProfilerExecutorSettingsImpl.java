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

import consulo.component.ProcessCanceledException;
import consulo.execution.configuration.RunProfile;
import consulo.execution.profiler.ProfilerExecutorSettings;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.execution.profiler.configuration.ProfilerStarter;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerExecutorSettingsImpl extends ProfilerExecutorSettings {
    private static final Logger LOG = Logger.getInstance(ProfilerExecutorSettingsImpl.class);

    private final ProfilerConfigurationTypeBase<?> myType;
    private final AtomicBoolean myStarterFailureLogged = new AtomicBoolean();
    private volatile ProfilerConfigurationState myState;
    private volatile ProfilerStarter myStarter;

    public ProfilerExecutorSettingsImpl(ProfilerConfigurationTypeBase<?> type, ProfilerConfigurationState state, ProfilerStarter starter) {
        myType = type;
        myState = state;
        myStarter = starter;
    }

    public ProfilerConfigurationTypeBase<?> getType() {
        return myType;
    }

    @Override
    public ProfilerConfigurationState getState() {
        return myState;
    }

    public ProfilerStarter getStarter() {
        return myStarter;
    }

    public boolean isSameConfiguration(ProfilerConfigurationTypeBase<?> type, ProfilerConfigurationState state) {
        return myType == type && Objects.equals(myState.getDisplayName(), state.getDisplayName());
    }

    public void update(ProfilerConfigurationState state, ProfilerStarter starter) {
        myStarter = starter;
        myState = state;
    }

    public LocalizeValue getPresentableName() {
        String displayName = myState.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return LocalizeValue.of(displayName);
        }
        return myType.getDisplayName();
    }

    @Override
    public Image getIcon() {
        return myType.getIcon();
    }

    @Override
    public LocalizeValue getActionName() {
        return getPresentableName();
    }

    @Override
    public LocalizeValue getStartActionText() {
        return LocalizeValue.lazy(() -> LocalizeValue.localizeTODO("Profile with '" + getPresentableName().get() + "'"));
    }

    @Override
    public LocalizeValue getStartActiveText(String configurationName) {
        if (StringUtil.isEmpty(configurationName)) {
            return getStartActionText();
        }
        return LocalizeValue.lazy(
            () -> LocalizeValue.localizeTODO("Profile '" + configurationName + "' with '" + getPresentableName().get() + "'")
        );
    }

    @Override
    public boolean isApplicable(Project project) {
        try {
            return myStarter.isApplicable(project);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            logStarterFailure(e);
            return false;
        }
    }

    @Override
    public boolean canRun(RunProfile profile) {
        try {
            return myStarter.canRun(profile);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            logStarterFailure(e);
            return false;
        }
    }

    private void logStarterFailure(Throwable e) {
        if (myStarterFailureLogged.compareAndSet(false, true)) {
            LOG.error("Profiler starter of " + myState + " failed", e);
        }
    }

    @Override
    public String toString() {
        return getPresentableName().get();
    }

    static @Nullable ProfilerStarter createStarter(ProfilerConfigurationTypeBase<?> type, ProfilerConfigurationState state) {
        try {
            return type.createStarterFor(state);
        }
        catch (Throwable e) {
            LOG.error("Profiler configuration type " + type.getId() + " failed to create a starter", e);
            return null;
        }
    }
}
