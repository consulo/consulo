/*
 * Copyright 2013-2022 consulo.io
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
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ChangesViewContentFactory {
  String getId();

  LocalizeValue getTabName();

  default boolean isAvailable() {
    return true;
  }

  default boolean isInCommitToolWindow() {
    return false;
  }

  /**
   * Position of the tab among the other tabs of the tool window: tabs with a smaller weight go first.
   */
  default int getOrderWeight() {
    return 100;
  }

  default boolean isCloseable() {
    return false;
  }

  /**
   * Called when the user closes the tab of a {@link #isCloseable() closeable} content.
   * It is not called when the tab is removed because {@link #isAvailable()} became {@code false}.
   */
  default void contentClosedByUser() {
  }

  
  ChangesViewContentProvider create();
}
