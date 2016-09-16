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
package consulo.ide.ui.laf.modern;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 05.08.14
 */
public class ModernTextAreaUI extends BasicTextAreaUI implements ModernTextBorder.ModernTextUI {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(final JComponent c) {
    final ModernTextAreaUI ui = new ModernTextAreaUI(c);
    c.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        ui.myFocused = true;
        c.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        ui.myFocused = false;
        c.repaint();
      }
    });

    return ui;
  }

  private boolean myFocused;
  private MouseEnterHandler myMouseEnterHandler;

  public ModernTextAreaUI(JComponent c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  public boolean isFocused() {
    return myFocused;
  }

  @NotNull
  @Override
  public MouseEnterHandler getMouseEnterHandler() {
    return myMouseEnterHandler;
  }
}
