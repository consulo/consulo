// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.dataholder;

import java.util.function.Supplier;

/**
 * Some part of JB utils only for {@link UserDataHolder}
 */
public class UserDataHolderUtil {
    /**
     * @return defaultValue if the reference contains null (in that case defaultValue is placed there), or reference value otherwise.
     */
    public static <T> T computeIfAbsent(UserDataHolder holder, Key<T> key, Supplier<? extends T> defaultValue) {
        T data = holder.getUserData(key);
        if (data != null) {
            return data;
        }

        if (holder instanceof UserDataHolderEx userDataHolderEx) {
            return userDataHolderEx.putUserDataIfAbsent(key, defaultValue.get());

        }
        return slowPath(holder, key, defaultValue);
    }

    // separate method to hint jvm not to inline this code, thus increasing chances of inlining the caller
    private static <T> T slowPath(UserDataHolder holder, Key<T> key, Supplier<? extends T> defaultValue) {
        T data;
        synchronized (holder) {
            data = holder.getUserData(key);
            if (data != null) {
                return data;
            }
            data = defaultValue.get();
            holder.putUserData(key, data);
            return data;
        }
    }
}
