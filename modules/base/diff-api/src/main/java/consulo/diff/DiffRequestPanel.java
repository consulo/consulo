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
package consulo.diff;

import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;

public interface DiffRequestPanel extends Disposable {
  void setRequest(@Nullable DiffRequest request);

  /*
   * Sets request to show.
   * Will not override current request, if their keys are not null and equal.
   */
  void setRequest(@Nullable DiffRequest request, @Nullable Object identity);

  @Nonnull
  JComponent getComponent();

  @Nullable
  JComponent getPreferredFocusedComponent();

  @RequiredUIAccess
  <T> void putContextHints(@Nonnull Key<T> key, @Nullable T value);
}
