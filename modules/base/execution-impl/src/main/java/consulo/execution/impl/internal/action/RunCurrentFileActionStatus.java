/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal.action;

import consulo.execution.RunnerAndConfigurationSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.List;

public record RunCurrentFileActionStatus(
    boolean enabled,
    @Nonnull LocalizeValue tooltip,
    @Nonnull Image icon,
    @Nonnull List<RunnerAndConfigurationSettings> runConfigs) {

    static @Nonnull RunCurrentFileActionStatus createDisabled(@Nonnull LocalizeValue tooltip,
                                                              @Nonnull Image icon) {
        return new RunCurrentFileActionStatus(false, tooltip, icon, List.of());
    }

    static @Nonnull RunCurrentFileActionStatus createEnabled(@Nonnull LocalizeValue tooltip,
                                                             @Nonnull Image icon,
                                                             @Nonnull List<RunnerAndConfigurationSettings> runConfigs) {
        return new RunCurrentFileActionStatus(true, tooltip, icon, runConfigs);
    }
}