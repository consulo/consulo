// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.dataholder;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Non thread safe version of {@link UserDataHolderBase}.
 */
public class UnprotectedUserDataHolder implements UserDataHolder {

  private Map<Key, Object> myUserData;

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    //noinspection unchecked
    T value = myUserData != null ? (T)myUserData.get(key) : null;
    if (value == null && key instanceof KeyWithDefaultValue) {
      value = ((KeyWithDefaultValue<T>)key).getDefaultValue();
      putUserData(key, value);
    }
    return value;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    if (myUserData == null) myUserData = new HashMap<>();
    myUserData.put(key, value);
  }
}
