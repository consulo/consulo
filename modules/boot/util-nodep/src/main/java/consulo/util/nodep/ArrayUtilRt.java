/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.nodep;

import org.jspecify.annotations.Nullable;

/**
 * Stripped-down version of {@code consulo.ide.impl.idea.util.ArrayUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "SSBasedInspection"})
public class ArrayUtilRt {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * @param src source array.
     * @param obj object to be found.
     * @return index of <code>obj</code> in the <code>src</code> array.
     * Returns <code>-1</code> if passed object isn't found. This method uses
     * <code>equals</code> of arrays elements to compare <code>obj</code> with
     * these elements.
     */
    public static <T> int find(T[] src, @Nullable T obj) {
        for (int i = 0; i < src.length; i++) {
            T o = src[i];
            if (o == null) {
                if (obj == null) {
                    return i;
                }
            }
            else if (o.equals(obj)) {
                return i;
            }
        }
        return -1;
    }
}
