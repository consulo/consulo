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
package consulo.execution.profiler.impl.internal.toolwindow;

import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.AttachableTargetProcess;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerConfigurationPresentation {
    private ProfilerConfigurationPresentation() {
    }

    public static LocalizeValue getName(ProfilerConfigurationState state) {
        String displayName = state.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return LocalizeValue.of(displayName);
        }

        ProfilerConfigurationTypeBase<?> type = ProfilerConfigurationTypeBase.findById(state.getConfigurationTypeId());
        return type == null ? LocalizeValue.of(state.getConfigurationTypeId()) : type.getDisplayName();
    }

    public static @Nullable Image getIcon(ProfilerConfigurationState state) {
        ProfilerConfigurationTypeBase<?> type = ProfilerConfigurationTypeBase.findById(state.getConfigurationTypeId());
        return type == null ? null : type.getIcon();
    }

    public static String getTargetName(ProfilerProcess<?> process) {
        AttachableTargetProcess target = process.getTargetProcess();
        String targetName = target.getFullName();
        if (target.getPid() > 0) {
            String pidSuffix = " (" + target.getPid() + ")";
            if (!targetName.endsWith(pidSuffix)) {
                return targetName + pidSuffix;
            }
        }
        return targetName;
    }

    public static String getTabName(ProfilerProcess<?> process) {
        StringBuilder builder = new StringBuilder(getTargetName(process));

        String configurationName = getName(process.getProfilerConfiguration()).get();
        if (!configurationName.isEmpty()) {
            builder.append(" - ").append(configurationName);
        }
        return builder.toString();
    }
}
