/*
 * Copyright 2013-2017 consulo.io
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

import consulo.localize.internal.DefaultLocalizeKey;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2017-11-09
 */
public interface LocalizeKey {
    static LocalizeKey of(String localizeId, String key) {
        return new DefaultLocalizeKey(localizeId, key.toLowerCase(Locale.ROOT));
    }
    static LocalizeKey of(String localizeId, String key, int argumentsCount) {
        // TODO [VISTALL] make optimization for future use on call #getValue()
        return new DefaultLocalizeKey(localizeId, key.toLowerCase(Locale.ROOT));
    }
    String getLocalizeId();
    String getKey();
    LocalizeValue getValue();
    LocalizeValue getValue(Object... args);
}
