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

import com.intellij.ui.ComponentUtil;
import com.intellij.util.ui.JBInsets;
import consulo.desktop.ui.laf.idea.darcula.DarculaEditorTextFieldBorder;
import consulo.desktop.ui.laf.idea.darcula.DarculaUIUtil;
import consulo.desktop.ui.laf.idea.darcula.TextFieldWithPopupHandlerUI;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author VISTALL
 * @since 2019-04-26
 */
public class ModernTextFieldUI extends TextFieldWithPopupHandlerUI implements ModernTextBorder.ModernTextUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernTextFieldUI((JTextField)c);
  }

  private final MouseEnterHandler myMouseEnterHandler;
  private boolean myFocus;
  private FocusListener myFocusListener;

  public ModernTextFieldUI(JTextField c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  protected Insets getDefaultMargins() {
    Component c = getComponent();
    return DarculaUIUtil.isCompact(c) || DarculaUIUtil.isTableCellEditor(c) ? JBInsets.create(0, 3) : JBInsets.create(2, 6);
  }


  @Override
  protected int getMinimumHeight(int textHeight) {
    Insets i = getComponent().getInsets();
    JComponent c = getComponent();
    int minHeight = (DarculaUIUtil.isCompact(c) ? DarculaUIUtil.COMPACT_HEIGHT.get() : DarculaUIUtil.MINIMUM_HEIGHT.get()) + i.top + i.bottom;
    return DarculaEditorTextFieldBorder.isComboBoxEditor(c) || ComponentUtil.getParentOfType((Class<? extends JSpinner>)JSpinner.class, (Component)c) != null ? textHeight : minHeight;
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

    c.removeFocusListener(myFocusListener);

    myMouseEnterHandler.replace(c, null);
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
