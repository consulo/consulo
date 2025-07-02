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
package consulo.compiler.util;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class ExportableUserDataHolderBase extends UserDataHolderBase implements ExportableUserDataHolder {
    private final Set<Key> myKeys = Collections.synchronizedSet(new HashSet<Key>());

    @Override
    @Nonnull
    public final Map<Key, Object> exportUserData() {
        final Map<Key, Object> result = new HashMap<Key, Object>();
        synchronized (myKeys) {
            for (Key<?> k : myKeys) {
                final Object data = getUserData(k);
                if (data != null) {
                    result.put(k, data);
                }
            }
        }
        return result;
    }

    @Override
    public final <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        if (value != null) {
            myKeys.add(key);
        }
        else {
            myKeys.remove(key);
        }
        super.putUserData(key, value);
    }

}
