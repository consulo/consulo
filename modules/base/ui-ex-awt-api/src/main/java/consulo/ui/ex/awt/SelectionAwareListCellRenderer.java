// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class SelectionAwareListCellRenderer<T> implements ListCellRenderer<T> {
  private final Function<? super T, ? extends JComponent> myFun;

  public SelectionAwareListCellRenderer(Function<? super T, ? extends JComponent> fun) {
    myFun = fun;
  }

  @Nonnull
  @Override
  public Component getListCellRendererComponent(@Nonnull JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    @SuppressWarnings({"unchecked"}) JComponent comp = myFun.apply((T)value);
    comp.setOpaque(true);
    if (isSelected) {
      comp.setBackground(list.getSelectionBackground());
      comp.setForeground(list.getSelectionForeground());
    }
    else {
      comp.setBackground(list.getBackground());
      comp.setForeground(list.getForeground());
    }
    for (JLabel label : UIUtil.findComponentsOfType(comp, JLabel.class)) {
      label.setForeground(UIUtil.getListForeground(isSelected));
    }
    return comp;
  }
}
