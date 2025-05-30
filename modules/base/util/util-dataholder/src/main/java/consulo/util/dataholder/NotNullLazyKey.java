/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.util.dataholder;

import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author peter
 */
public class NotNullLazyKey<T, H extends UserDataHolder> extends Key<T> {
    private final Function<H, T> myFunction;

    @SuppressWarnings("deprecation")
    private NotNullLazyKey(@Nonnull String name, @Nonnull Function<H, T> function) {
        super(name);
        myFunction = function;
    }

    @Nonnull
    public final T getValue(@Nonnull H h) {
        T data = h.getUserData(this);
        if (data == null) {
            h.putUserData(this, data = myFunction.apply(h));
        }
        return data;
    }

    public static <T, H extends UserDataHolder> NotNullLazyKey<T, H> create(@Nonnull String name, @Nonnull Function<H, T> function) {
        return new NotNullLazyKey<>(name, function);
    }
}
