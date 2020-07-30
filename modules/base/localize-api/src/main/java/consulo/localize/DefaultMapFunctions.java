/*
 * Copyright 2013-2020 consulo.io
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
package consulo.localize;

import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
class DefaultMapFunctions {
  static final BiFunction<LocalizeManager, String, String> TO_UPPER_CASE = (localizeManager, s) -> s.toUpperCase(localizeManager.getLocale());

  static final BiFunction<LocalizeManager, String, String> TO_LOWER_CASE = (localizeManager, s) -> s.toLowerCase(localizeManager.getLocale());
}
