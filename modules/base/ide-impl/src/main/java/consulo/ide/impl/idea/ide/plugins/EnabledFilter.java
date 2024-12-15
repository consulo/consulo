/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide.plugins;

import consulo.ide.impl.localize.PluginLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
* @author UNV
* @since 2024-11-14
*/
public enum EnabledFilter {
    ALL_PLUGINS(PluginLocalize.enabledFilterAllPlugins()) {
        @Override
        public boolean accepts(Boolean enabled) {
            return true;
        }
    },
    ENABLED(PluginLocalize.enabledFilterEnabledPlugins()) {
        @Override
        public boolean accepts(Boolean enabled) {
            return enabled != null && enabled;
        }
    },
    DISABLED(PluginLocalize.enabledFilterDisabledPlugins()) {
        @Override
        public boolean accepts(Boolean enabled) {
            return enabled != null && !enabled;
        }
    };

    private final LocalizeValue myTitle;

    EnabledFilter(@Nonnull LocalizeValue title) {
        myTitle = title;
    }

    public abstract boolean accepts(Boolean enabled);

    @Nonnull
    public LocalizeValue getTitle() {
        return myTitle;
    }
}
