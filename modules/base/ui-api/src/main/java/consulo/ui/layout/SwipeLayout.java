/*
 * Copyright 2013-2018 consulo.io
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

import consulo.ui.internal.UIInternal;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-07-01
 */
public interface SwipeLayout extends Layout {
  @Nonnull
  static SwipeLayout create() {
    return UIInternal.get()._Layouts_swipe();
  }

  @Nonnull
  default SwipeLayout register(@Nonnull String id, @Nonnull Layout layout) {
    return register(id, () -> layout);
  }

  @Nonnull
  SwipeLayout register(@Nonnull String id, @Nonnull @RequiredUIAccess Supplier<Layout> layoutSupplier);

  /**
   * @param id of child
   * @return child layout which will be showed
   */
  @Nonnull
  Layout swipeLeftTo(@Nonnull String id);

  /**
   * @param id of child
   * @return child layout which will be showed
   */
  @Nonnull
  Layout swipeRightTo(@Nonnull String id);
}
