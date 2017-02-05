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
 * @since 15-Jun-16
 */
public interface LabeledLayout extends Layout {
  @NotNull
  @RequiredUIAccess
  default LabeledLayout set(@NotNull PseudoComponent component) {
    return set(component.getComponent());
  }

  @NotNull
  @RequiredUIAccess
  LabeledLayout set(@NotNull Component component);
}
