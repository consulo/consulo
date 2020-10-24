/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public interface ThreeComponentSplitLayout extends Layout {
  @Nonnull
  static ThreeComponentSplitLayout create(@Nonnull SplitLayoutPosition position) {
    return UIInternal.get()._ThreeComponentSplitLayout_create(position);
  }

  @RequiredUIAccess
  @Nonnull
  ThreeComponentSplitLayout setFirstComponent(@Nullable Component component);

  @RequiredUIAccess
  @Nonnull
  ThreeComponentSplitLayout setCenterComponent(@Nullable Component component);

  @RequiredUIAccess
  @Nonnull
  ThreeComponentSplitLayout setSecondComponent(@Nullable Component component);
}
