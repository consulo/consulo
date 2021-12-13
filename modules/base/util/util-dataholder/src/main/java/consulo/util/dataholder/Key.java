/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.util.dataholder.internal.KeyRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Provides type-safe access to data.
 *
 * @author max
 * @author Konstantin Bulenkov
 */
public class Key<T> {
  private static final KeyRegistry ourRegistry = KeyRegistry.ourInstance;

  private final String myName; // for debug purposes only
  private final int myIndex;

  @Deprecated
  @DeprecationInfo("Use #create(name)")
  public Key(@Nonnull String name) {
    myName = name;
    myIndex = ourRegistry.register(this);
  }

  // made final because many classes depend on one-to-one key index <-> key instance relationship. See e.g. UserDataHolderBase
  @Override
  public final int hashCode() {
    return myIndex;
  }

  @Override
  public final boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Nonnull
  @SuppressWarnings("deprecation")
  public static <T> Key<T> create(@Nonnull Class<? extends T> clazz) {
    return new Key<>(clazz.getName());
  }

  @Nonnull
  @SuppressWarnings("deprecation")
  public static <T> Key<T> create(@Nonnull String name) {
    return new Key<>(name);
  }

  public T get(@Nullable UserDataHolder holder) {
    return holder == null ? null : holder.getUserData(this);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public T get(@Nullable Map<Key, ?> holder) {
    return holder == null ? null : (T)holder.get(this);
  }

  public T get(@Nullable UserDataHolder holder, T defaultValue) {
    final T t = get(holder);
    return t == null ? defaultValue : t;
  }

  /**
   * Returns {@code true} if and only if the {@code holder} has
   * not null value by the key.
   *
   * @param holder user data holder object
   * @return {@code true} if holder.getUserData(this) != null
   * {@code false} otherwise.
   */
  public boolean isIn(@Nullable UserDataHolder holder) {
    return get(holder) != null;
  }

  public void set(@Nullable UserDataHolder holder, @Nullable T value) {
    if (holder != null) {
      holder.putUserData(this, value);
    }
  }

  public void set(@Nullable Map<Key, Object> holder, T value) {
    if (holder != null) {
      holder.put(this, value);
    }
  }
}