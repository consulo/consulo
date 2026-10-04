// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.grid;

/**
 * How an {@link ObjectFormatter} formats a value: its {@link ObjectFormatterMode mode} and what it may show. It carries no grid
 * settings - they live in {@code consulo.ui.ex.api}, which this module does not see. A config which carries them extends this
 * interface there, and code there which needs the settings of a config, like the formatters of the cell editors, asks for that
 * subtype and treats any other config as one without settings.
 */
public interface ObjectFormatterConfig {
    static ObjectFormatterConfig of(ObjectFormatterMode mode) {
        return of(mode, false);
    }

    static ObjectFormatterConfig of(ObjectFormatterMode mode, boolean allowedShowBigObjects) {
        return new SimpleObjectFormatterConfig(mode, allowedShowBigObjects, false);
    }

    ObjectFormatterMode getMode();

    boolean isAllowedShowBigObjects();

    boolean supportsNumberFormats();
}
