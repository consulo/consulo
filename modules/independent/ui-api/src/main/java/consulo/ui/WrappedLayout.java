/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public interface WrappedLayout extends Layout {
  @NotNull
  static WrappedLayout create() {
    return UIInternal.get()._Layouts_wrapped();
  }

  @NotNull
  @RequiredUIAccess
  static WrappedLayout create(@NotNull Component component) {
    return UIInternal.get()._Layouts_wrapped().set(component);
  }

  @RequiredUIAccess
  @NotNull
  WrappedLayout set(@Nullable Component component);

  @RequiredUIAccess
  @NotNull
  default WrappedLayout set(@NotNull PseudoComponent component) {
    return set(component.getComponent());
  }
}
