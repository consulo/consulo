/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.Convertor;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
@Deprecated
@DeprecationInfo(value = "Use com.intellij.util.ObjectUtil")
public class ObjectUtils {
  private ObjectUtils() {
  }

  public static final Object NULL = ObjectUtil.NULL;

  @Nonnull
  public static <T> T assertNotNull(@Nullable T t) {
    return notNull(t);
  }

  public static <T> void assertAllElementsNotNull(@Nonnull T[] array) {
    for (int i = 0; i < array.length; i++) {
      T t = array[i];
      if (t == null) {
        throw new NullPointerException("Element [" + i + "] is null");
      }
    }
  }

  @Contract("null, null -> null")
  public static <T> T chooseNotNull(@Nullable T t1, @Nullable T t2) {
    return t1 == null? t2 : t1;
  }

  @Contract("null,null->null")
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2) {
    return t1 != null ? t1 : t2;
  }

  @Contract("null,null,null->null")
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2, @Nullable T t3) {
    return t1 != null ? t1 : t2 != null ? t2 : t3;
  }

  @Nullable
  public static <T> T coalesce(@Nullable Iterable<T> o) {
    if (o == null) return null;
    for (T t : o) {
      if (t != null) return t;
    }
    return null;
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value) {
    //noinspection ConstantConditions
    return notNull(value, value);
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value, @Nonnull T defaultValue) {
    return value == null ? defaultValue : value;
  }

  @Nullable
  public static <T> T tryCast(@Nullable Object obj, @Nonnull Class<T> clazz) {
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    return null;
  }

  @Nullable
  public static <T, S> S doIfCast(@Nullable Object obj, @Nonnull Class<T> clazz, final Convertor<T, S> convertor) {
    if (clazz.isInstance(obj)) {
      //noinspection unchecked
      return convertor.convert((T)obj);
    }
    return null;
  }

  @Nullable
  public static <T> T nullizeByCondition(@Nullable final T obj, @Nonnull final Condition<T> condition) {
    if (condition.value(obj)) {
      return null;
    }
    return obj;
  }
}
