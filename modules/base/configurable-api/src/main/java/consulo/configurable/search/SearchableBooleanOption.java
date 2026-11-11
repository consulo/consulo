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
package consulo.configurable.search;

import consulo.localize.LocalizeValue;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A check box of a settings page - its text, and the setting it shows.
 *
 * @param text   the text of the check box
 * @param getter reads the setting
 * @param setter writes the setting, and tells whoever needs to know
 * @author VISTALL
 * @since 2026-11-11
 */
public record SearchableBooleanOption(LocalizeValue text, BooleanSupplier getter, Consumer<Boolean> setter) {
}
