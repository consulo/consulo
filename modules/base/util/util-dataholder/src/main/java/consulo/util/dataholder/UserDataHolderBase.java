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

import consulo.util.dataholder.keyFMap.KeyFMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class UserDataHolderBase implements UserDataHolderEx, Cloneable {
  public static final Key<KeyFMap> COPYABLE_USER_MAP_KEY = Key.create("COPYABLE_USER_MAP_KEY");

  private static VarHandle ourUpdaterVarHandle;

  static {
    try {
      ourUpdaterVarHandle = MethodHandles.lookup().findVarHandle(UserDataHolderBase.class, "myUserMap", KeyFMap.class);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new Error(e);
    }
  }

  /**
   * Concurrent writes to this field are via CASes only, using the {@link #updater}
   */
  @Nonnull
  private volatile KeyFMap myUserMap = KeyFMap.EMPTY_MAP;

  @Override
  protected Object clone() {
    try {
      UserDataHolderBase clone = (UserDataHolderBase)super.clone();
      clone.setUserMap(KeyFMap.EMPTY_MAP);
      copyCopyableDataTo(clone);
      return clone;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Test Only
   */
  public String getUserDataString() {
    final KeyFMap userMap = getUserMap();
    final KeyFMap copyableMap = getUserData(COPYABLE_USER_MAP_KEY);
    return userMap.toString() + (copyableMap == null ? "" : copyableMap.toString());
  }

  public void copyUserDataTo(UserDataHolderBase other) {
    other.setUserMap(getUserMap());
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    //noinspection unchecked
    return getUserMap().get(key);
  }

  @Nonnull
  protected KeyFMap getUserMap() {
    return myUserMap;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    while (true) {
      KeyFMap map = getUserMap();
      KeyFMap newMap = value == null ? map.minus(key) : map.plus(key, value);
      if (newMap == map || changeUserMap(map, newMap)) {
        break;
      }
    }
  }

  protected boolean changeUserMap(KeyFMap oldMap, KeyFMap newMap) {
    return ourUpdaterVarHandle.compareAndSet(this, oldMap, newMap);
  }

  public <T> T getCopyableUserData(Key<T> key) {
    KeyFMap map = getUserData(COPYABLE_USER_MAP_KEY);
    //noinspection unchecked,ConstantConditions
    return map == null ? null : map.get(key);
  }

  public <T> void putCopyableUserData(Key<T> key, T value) {
    while (true) {
      KeyFMap map = getUserMap();
      KeyFMap copyableMap = map.get(COPYABLE_USER_MAP_KEY);
      if (copyableMap == null) {
        copyableMap = KeyFMap.EMPTY_MAP;
      }
      KeyFMap newCopyableMap = value == null ? copyableMap.minus(key) : copyableMap.plus(key, value);
      KeyFMap newMap = newCopyableMap.isEmpty() ? map.minus(COPYABLE_USER_MAP_KEY) : map.plus(COPYABLE_USER_MAP_KEY, newCopyableMap);
      if (newMap == map || changeUserMap(map, newMap)) {
        return;
      }
    }
  }

  @Override
  public <T> boolean replace(@Nonnull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    while (true) {
      KeyFMap map = getUserMap();
      if (map.get(key) != oldValue) {
        return false;
      }
      KeyFMap newMap = newValue == null ? map.minus(key) : map.plus(key, newValue);
      if (newMap == map || changeUserMap(map, newMap)) {
        return true;
      }
    }
  }

  @Override
  @Nonnull
  public <T> T putUserDataIfAbsent(@Nonnull final Key<T> key, @Nonnull final T value) {
    while (true) {
      KeyFMap map = getUserMap();
      T oldValue = map.get(key);
      if (oldValue != null) {
        return oldValue;
      }
      KeyFMap newMap = map.plus(key, value);
      if (newMap == map || changeUserMap(map, newMap)) {
        return value;
      }
    }
  }

  public void copyCopyableDataTo(@Nonnull UserDataHolderBase clone) {
    clone.putUserData(COPYABLE_USER_MAP_KEY, getUserData(COPYABLE_USER_MAP_KEY));
  }

  protected void clearUserData() {
    setUserMap(KeyFMap.EMPTY_MAP);
  }

  protected void setUserMap(@Nonnull KeyFMap map) {
    myUserMap = map;
  }

  public boolean isUserDataEmpty() {
    return getUserMap().isEmpty();
  }
}
