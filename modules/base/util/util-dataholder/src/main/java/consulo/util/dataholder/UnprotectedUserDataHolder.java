// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.dataholder;


import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Non thread safe version of {@link UserDataHolderBase}.
 */
public class UnprotectedUserDataHolder implements UserDataHolder {
    private @Nullable Map<Key, Object> myUserData;

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getUserData(Key<T> key) {
        T value = myUserData != null ? (T)myUserData.get(key) : null;
        if (value == null && key instanceof KeyWithDefaultValue keyWithDefaultValue) {
            value = (T)keyWithDefaultValue.getDefaultValue();
            putUserData(key, value);
        }
        return value;
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        if (myUserData == null) {
            myUserData = new HashMap<>();
        }
        myUserData.put(key, value);
    }
}
