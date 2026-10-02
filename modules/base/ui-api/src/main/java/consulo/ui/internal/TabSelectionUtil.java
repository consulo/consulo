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
package consulo.ui.internal;

import java.util.function.IntPredicate;

public final class TabSelectionUtil {
    private TabSelectionUtil() {
    }

    public static int findSelectionOnDisable(int index, int count, IntPredicate enabled) {
        if (index < 0 || index >= count || count == 1) {
            return -1;
        }

        for (int i = index - 1; i >= 0; i--) {
            if (enabled.test(i)) {
                return i;
            }
        }

        for (int i = index + 1; i < count; i++) {
            if (enabled.test(i)) {
                return i;
            }
        }

        return -1;
    }
}
