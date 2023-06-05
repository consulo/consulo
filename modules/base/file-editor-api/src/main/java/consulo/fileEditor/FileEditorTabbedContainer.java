/*
 * Copyright 2013-2023 consulo.io
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
package consulo.fileEditor;

import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02/06/2023
 */
public interface FileEditorTabbedContainer {
  ActionCallback setSelectedIndex(int index);

  @Nonnull
  ActionCallback setSelectedIndex(final int indexToSelect, boolean focusEditor);

  int getSelectedIndex();

  int getTabCount();

  @Nonnull
  default ActionCallback removeTabAt(final int componentIndex, int indexToSelect) {
    return removeTabAt(componentIndex, indexToSelect, true);
  }

  @Nonnull
  ActionCallback removeTabAt(final int componentIndex, int indexToSelect, boolean transferFocus);

  /**
   * Close selected tab if exists
   */
  void close();
}
