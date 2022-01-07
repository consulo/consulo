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
package com.intellij.openapi.actionSystem;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// We implement UserDataHolder to support DataManager.saveInDataContext/loadFromDataContext methods
public class DataContextWrapper implements DataContext, UserDataHolder {
  private final DataContext myDelegate;
  private final UserDataHolder myDataHolder;

  public DataContextWrapper(@Nonnull DataContext delegate) {
    myDelegate = delegate;
    myDataHolder = delegate instanceof UserDataHolder ? (UserDataHolder) delegate : new UserDataHolderBase();
  }

  @Nullable
  @Override
  public <T> T getData(@Nonnull Key<T> dataId) {
    return myDelegate.getData(dataId);
  }

  @javax.annotation.Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myDataHolder.putUserData(key, value);
  }
}
