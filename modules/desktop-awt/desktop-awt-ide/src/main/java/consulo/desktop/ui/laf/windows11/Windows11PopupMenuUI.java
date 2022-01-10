/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.ui.laf.windows11;

import com.intellij.ide.ui.laf.intellij.IdeaPopupMenuUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class Windows11PopupMenuUI extends IdeaPopupMenuUI {

  public static ComponentUI createUI(final JComponent c) {
    return new Windows11PopupMenuUI();
  }

  @Override
  public boolean isRoundBorder() {
    return true;
  }
}
