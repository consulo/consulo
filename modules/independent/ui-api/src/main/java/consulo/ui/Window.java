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
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface Window extends Component {
  @NotNull
  static Window createModal(@NotNull String title) {
    return UIInternal.get()._Windows_modalWindow(title);
  }

  @RequiredUIAccess
  void setTitle(@NotNull String title);

  @RequiredUIAccess
  void setContent(@NotNull Component content);

  @RequiredUIAccess
  void setMenuBar(@Nullable MenuBar menuBar);

  void setResizable(boolean value);

  void setClosable(boolean value);

  /**
   * not block current thread
   */
  @RequiredUIAccess
  void show();

  @RequiredUIAccess
  void close();
}
