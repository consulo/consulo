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
package consulo.localize.internal;

import consulo.localize.LocalizeManager;

import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
public class DefaultMapFunctions {
    public static final BiFunction<LocalizeManager, String, String> TO_UPPER_CASE = (localizeManager, s) -> s.toUpperCase(localizeManager.getLocale());

    public static final BiFunction<LocalizeManager, String, String> TO_LOWER_CASE = (localizeManager, s) -> s.toLowerCase(localizeManager.getLocale());

    public static final BiFunction<LocalizeManager, String, String> CAPTILIZE = (localizeManager, s) -> {
        if (s.isEmpty()) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase(localizeManager.getLocale());
        }
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    };
}
