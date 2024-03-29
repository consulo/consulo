/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.fileChooser;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface FileChooserDialog {
  Key<Boolean> PREFER_LAST_OVER_TO_SELECT = PathChooserDialog.PREFER_LAST_OVER_EXPLICIT;

  /**
   * @deprecated Please use {@link #choose(ComponentManager, VirtualFile...)} because
   * it supports several selections
   */
  @Deprecated
  @Nonnull
  default VirtualFile[] choose(@Nullable VirtualFile toSelect, @Nullable ComponentManager project) {
    return toSelect == null ? choose(project) : choose(project, toSelect);
  }

  /**
   * Choose one or more files
   *
   * @param project  use this project (you may pass null if you already set project in ctor)
   * @param toSelect files to be selected automatically.
   * @return files chosen by user
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #chooseAsync")
  default VirtualFile[] choose(@Nullable ComponentManager project, @Nonnull VirtualFile... toSelect) {
    throw new UnsupportedOperationException("desktop only");
  }

  /**
   * Choose one or more files
   *
   * @param project  use this project (you may pass null if you already set project in ctor)
   * @param toSelect files to be selected automatically.
   */
  @RequiredUIAccess
  @Nonnull
  default AsyncResult<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, @Nonnull VirtualFile[] toSelect) {
    throw new AbstractMethodError();
  }
}