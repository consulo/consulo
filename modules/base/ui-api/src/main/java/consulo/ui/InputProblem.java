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
package consulo.ui;

import consulo.localize.LocalizeValue;

/**
 * What is wrong with a value being entered. A warning is shown but still lets the box be confirmed;
 * anything else blocks it.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public record InputProblem(LocalizeValue message, NotificationType type) {
    public static InputProblem error(LocalizeValue message) {
        return new InputProblem(message, NotificationType.ERROR);
    }

    public static InputProblem warning(LocalizeValue message) {
        return new InputProblem(message, NotificationType.WARNING);
    }

    public boolean blocksConfirm() {
        return type != NotificationType.WARNING;
    }
}
