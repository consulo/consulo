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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface Window extends Component {
  static interface CloseListener extends EventListener {
    @RequiredUIAccess
    void onClose();
  }

  @Nonnull
  static Window create(@Nonnull String title, @Nonnull WindowOptions options) {
    return UIInternal.get()._Window_create(title, options);
  }

  @Nullable
  static Window getActiveWindow() {
    return UIInternal.get()._Window_getActiveWindow();
  }

  @Nullable
  static Window getFocusedWindow() {
    return UIInternal.get()._Window_getFocusedWindow();
  }

  @RequiredUIAccess
  void setTitle(@Nonnull String title);

  @Nullable
  @Override
  Window getParent();

  @RequiredUIAccess
  void setContent(@Nonnull Component content);

  @RequiredUIAccess
  void setMenuBar(@Nullable MenuBar menuBar);

  /**
   * not block current thread
   */
  @RequiredUIAccess
  void show();

  /**
   * Close and dispose resources. Window can't be opened second time
   */
  @RequiredUIAccess
  void close();
}
