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
package consulo.ui.web.internal;

import com.vaadin.ui.UI;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public class WebUIThreadLocal {
  private static final ThreadLocal<UI> ourUI = new ThreadLocal<>();

  public static void setUI(UI ui) {
    ourUI.set(ui);
  }

  @Nonnull
  public static UI getUI() {
    UI ui = ourUI.get();
    if (ui != null) {
      return ui;
    }
    UI current = UI.getCurrent();
    if (current != null) {
      return current;
    }
    throw new UnsupportedOperationException();
  }
}
