// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.util;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

public record KeyValuePair<T>(Key<T> key, @Nullable T value) {
    public static <T> void putData(UserDataHolder holder, KeyValuePair<T> keyValue) {
        holder.putUserData(keyValue.key(), keyValue.value());
    }

    public static void clearData(UserDataHolder holder, KeyValuePair<?> keyValue) {
        holder.putUserData(keyValue.key(), null);
    }
}
