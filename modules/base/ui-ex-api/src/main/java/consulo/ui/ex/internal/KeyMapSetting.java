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
package consulo.ui.ex.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.platform.Platform;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface KeyMapSetting {
    default boolean isUseUnicodeShortcutsWithDefault() {
        Boolean shortcuts = isUseUnicodeShortcuts();
        if (shortcuts != null) {
            return shortcuts;
        }

        return Platform.current().os().isMac();
    }

    Boolean isUseUnicodeShortcuts();

    void setUseUnicodeShortcuts(@Nullable Boolean value);

    boolean isEnabledDoublePressShortcuts();

    void setEnabledDoublePressShortcuts(boolean enabledDoublePressShortcuts);
}
