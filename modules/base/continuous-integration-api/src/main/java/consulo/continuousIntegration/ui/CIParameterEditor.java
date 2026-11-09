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
package consulo.continuousIntegration.ui;

import consulo.continuousIntegration.CIParameterValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Editor of one parameter in the run dialog.
 *
 * @since 2026-11-09
 */
public interface CIParameterEditor {
    Component getComponent();

    /**
     * {@code null} means the parameter is not sent, so the server uses its default.
     */
    @RequiredUIAccess
    @Nullable
    CIParameterValue getValue();

    /**
     * Shown without a label next to it (separators, notes).
     */
    default boolean isFullWidth() {
        return false;
    }
}
