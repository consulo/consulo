/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.configuration;

import consulo.application.Application;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class ConfigurationTypeUtil {
    private ConfigurationTypeUtil() {
    }

    @Nonnull
    public static <T extends ConfigurationType> T findConfigurationType(@Nonnull Class<T> configurationTypeClass) {
        return Application.get().getExtensionPoint(ConfigurationType.class).findExtensionOrFail(configurationTypeClass);
    }

    public static boolean equals(@Nonnull ConfigurationType type1, @Nonnull ConfigurationType type2) {
        return type1.getId().equals(type2.getId());
    }

    @Nullable
    public static ConfigurationType findConfigurationType(String configurationId) {
        return Application.get().getExtensionPoint(ConfigurationType.class)
            .findFirstSafe(type -> type.getId().equals(configurationId));
    }
}
