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
package com.intellij.diff;

import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class DiffContext implements UserDataHolder {
  protected final UserDataHolderBase myUserDataHolder = new UserDataHolderBase();

  @Nullable
  public abstract Project getProject();

  public abstract boolean isWindowFocused();

  public abstract boolean isFocused();

  public abstract void requestFocus();

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myUserDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myUserDataHolder.putUserData(key, value);
  }
}
