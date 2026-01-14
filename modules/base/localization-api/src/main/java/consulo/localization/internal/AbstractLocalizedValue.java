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
package consulo.localization.internal;

import consulo.localization.LocalizedValue;

import java.util.Comparator;

/**
 * @author UNV
 * @since 2025-11-18
 */
public abstract class AbstractLocalizedValue implements LocalizedValue {
    public static final Comparator<LocalizedValue> CASE_INSENSITIVE_ORDER = (lv1, lv2) -> {
        String v1 = lv1.get(), v2 = lv2.get();
        int insensitive = v1.compareToIgnoreCase(v2);
        return insensitive != 0 ? insensitive : v1.compareTo(v2);
    };

    @Override
    public String toString() {
        return getValue();
    }
}
