/*
 * Copyright 2013-2016 consulo.io
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

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface DockLayout extends Layout {
  @NotNull
  @RequiredUIAccess
  default DockLayout top(@NotNull PseudoComponent component) {
    return top(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  default DockLayout bottom(@NotNull PseudoComponent component) {
    return bottom(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  default DockLayout center(@NotNull PseudoComponent component) {
    return center(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  default DockLayout left(@NotNull PseudoComponent component) {
    return left(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  default DockLayout right(@NotNull PseudoComponent component) {
    return right(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  DockLayout top(@NotNull Component component);

  @NotNull
  @RequiredUIAccess
  DockLayout bottom(@NotNull Component component);

  @NotNull
  @RequiredUIAccess
  DockLayout center(@NotNull Component component);

  @NotNull
  @RequiredUIAccess
  DockLayout left(@NotNull Component component);

  @NotNull
  @RequiredUIAccess
  DockLayout right(@NotNull Component component);
}
