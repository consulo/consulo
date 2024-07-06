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
package consulo.diff.merge;

import consulo.diff.DiffUserDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class MergeContext implements UserDataHolder {
  protected final UserDataHolderBase myUserDataHolder = new UserDataHolderBase();

  @Nullable
  public abstract Project getProject();

  public abstract boolean isFocused();

  public abstract void requestFocus();

  /**
   * Called by MergeTool on conflict resolve end. Should delegate to the {@link MergeRequest#applyResult(MergeResult)}
   */
  @RequiredUIAccess
  public abstract void finishMerge(@Nonnull MergeResult result);

  /**
   * @see DiffUserDataKeys
   */
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
