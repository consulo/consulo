/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.SimpleTextAttributes;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author Vladimir Kondratyev
 */
public abstract class ColoredListCellRenderer<T> extends SimpleColoredComponent implements ListCellRenderer<T> {
  protected boolean mySelected;
  protected Color myForeground;
  protected Color mySelectionForeground;
  @Nullable
  private final JComboBox myComboBox;

  private boolean mySeparator;
  private String mySeparatorText;

  public ColoredListCellRenderer() {
    this(null);
  }

  public ColoredListCellRenderer(@Nullable JComboBox comboBox) {
    myComboBox = comboBox;
    
    setFocusBorderAroundIcon(true);
    setIpad(JBUI.insets(0, UIUtil.getListCellHPadding()));
  }

  public void setSeparator(@Nullable String text) {
    mySeparator = true;
    mySeparatorText = text;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
    clear();
    mySeparator = false;
    mySeparatorText = null;

    if (myComboBox != null) {
      setEnabled(myComboBox.isEnabled());
    }

    setFont(list.getFont());

    mySelected = selected;
    myForeground = isEnabled() ? list.getForeground() : UIManager.getColor("Label.disabledForeground");
    mySelectionForeground = list.getSelectionForeground();

    if (index == -1) {
      setOpaque(false);
      mySelected = false;
    }
    else {
      setOpaque(true);
      setBackground(selected ? list.getSelectionBackground() : null);
    }

    setPaintFocusBorder(hasFocus);

    customizeCellRenderer(list, value, index, selected, hasFocus);

    if (mySeparator) {
      final TitledSeparator separator = new TitledSeparator(mySeparatorText);
      separator.setBorder(JBUI.Borders.empty(0, 2, 0, 0));
      separator.setOpaque(false);
      separator.setBackground(UIUtil.TRANSPARENT_COLOR);
      separator.getLabel().setOpaque(false);
      separator.getLabel().setBackground(UIUtil.TRANSPARENT_COLOR);
      return separator;
    }

    return this;
  }

  /**
   * When the item is selected then we use default tree's selection foreground.
   * It guaranties readability of selected text in any LAF.
   */
  @Override
  public final void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, boolean isMainText) {
    if (mySelected) {
      super.append(fragment, new SimpleTextAttributes(attributes.getStyle(), mySelectionForeground), isMainText);
    }
    else if (attributes.getFgColor() == null) {
      super.append(fragment, new SimpleTextAttributes(attributes.getStyle(), myForeground), isMainText);
    }
    else {
      super.append(fragment, attributes, isMainText);
    }
  }

  protected abstract void customizeCellRenderer(@Nonnull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus);

  /**
   * Copied AS IS
   *
   * @see javax.swing.DefaultListCellRenderer#isOpaque()
   */
  @Override
  public boolean isOpaque() {
    Color back = getBackground();
    Component p = getParent();
    if (p != null) {
      p = p.getParent();
    }
    // p should now be the JList.
    boolean colorMatch = (back != null) && (p != null) &&
                         back.equals(p.getBackground()) &&
                         p.isOpaque();
    return !colorMatch && super.isOpaque();
  }
}
