/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.ui.laf.modern;

import com.intellij.ide.ui.BasicEditorTextFieldUI;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author VISTALL
 * @since 2019-04-26
 */
public class ModernEditorTextFieldUI extends BasicEditorTextFieldUI implements ModernTextBorder.ModernTextUI {
  public static ModernEditorTextFieldUI createUI(JComponent c) {
    return new ModernEditorTextFieldUI(c);
  }

  private final MouseEnterHandler myMouseEnterHandler;

  private boolean myFocus;
  private FocusListener myFocusListener;

  public ModernEditorTextFieldUI(JComponent textField) {
    myMouseEnterHandler = new MouseEnterHandler(textField);
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    myMouseEnterHandler.replace(null, c);

    c.addFocusListener(myFocusListener = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        myFocus = true;
        c.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        myFocus = false;
        c.repaint();
      }
    });
  }

  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);

    myMouseEnterHandler.replace(c, null);

    c.removeFocusListener(myFocusListener);
  }

  @Override
  public boolean isFocused() {
    return myFocus;
  }

  @Nonnull
  @Override
  public MouseEnterHandler getMouseEnterHandler() {
    return myMouseEnterHandler;
  }
}
