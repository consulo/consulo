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
package consulo.application;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SystemHealthMonitor {
    interface Reporter {
        void warning(@Nonnull String notificationText, @Nonnull String actionText, @Nonnull Runnable actionListener);
    }

    void check(@Nonnull Reporter reporter);
}
