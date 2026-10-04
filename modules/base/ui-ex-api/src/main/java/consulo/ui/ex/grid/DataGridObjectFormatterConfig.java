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
package consulo.ui.ex.grid;

import consulo.ui.grid.ObjectFormatterConfig;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ObjectFormatterConfig} which carries the grid settings, which {@code consulo.ui.grid} cannot declare because the settings
 * live in this module.
 * <p/>
 * Code which needs the settings of a config asks {@link #getSettings(ObjectFormatterConfig)}, so any other config is one without
 * settings. The formatters then use their intrinsic patterns.
 * <p/>
 * A config is a key of the formats cache ({@code consulo.ui.ex.grid.editor.FormatsCache}), so an implementation defines
 * {@code equals} and {@code hashCode}.
 *
 * @since 2026-10-04
 */
public interface DataGridObjectFormatterConfig extends ObjectFormatterConfig {
    @Nullable
    DataGridSettings getSettings();

    /**
     * @return the settings the config carries, or {@code null} for a config without settings, and for no config
     */
    static @Nullable DataGridSettings getSettings(@Nullable ObjectFormatterConfig config) {
        return config instanceof DataGridObjectFormatterConfig dataGridConfig ? dataGridConfig.getSettings() : null;
    }
}
