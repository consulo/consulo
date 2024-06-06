/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ComboBoxWithWidePopup<E> extends JComboBox<E> {

  private boolean myLayingOut = false;
  private int myMinLength = 20;

  public ComboBoxWithWidePopup(final ComboBoxModel<E> aModel) {
    super(aModel);

    if (Platform.current().os().isMac() && UIUtil.isUnderAquaLookAndFeel()) setMaximumRowCount(25);
  }

  public ComboBoxWithWidePopup(final E[] items) {
    super(items);

    if (Platform.current().os().isMac() && UIUtil.isUnderAquaLookAndFeel()) setMaximumRowCount(25);
  }

  public ComboBoxWithWidePopup(final Vector<E> items) {
    super(items);

    if (Platform.current().os().isMac() && UIUtil.isUnderAquaLookAndFeel()) setMaximumRowCount(25);
  }

  public ComboBoxWithWidePopup() {
  }
  
  @SuppressWarnings("GtkPreferredJComboBoxRenderer")
  @Override
  public void setRenderer(ListCellRenderer renderer) {
    super.setRenderer(new AdjustingListCellRenderer(this, renderer));
  }

  public void setMinLength(int minLength) {
    myMinLength = minLength;
  }

  /**
   * @return min string len to show
   */
  protected int getMinLength() {
    return myMinLength;
  }

  public void doLayout() {
    try {
      myLayingOut = true;
      super.doLayout();
    }
    finally {
      myLayingOut = false;
    }
  }

  public Dimension getSize() {
    Dimension size = super.getSize();
    if (!myLayingOut) {
      size.width = Math.max(size.width, getOriginalPreferredSize().width);
    }
    return size;
  }
  
  private Dimension _getSuperSize() {
    return super.getSize();
  }

  protected Dimension getOriginalPreferredSize() {
    return getPreferredSize();
  }

  /**
   * @return minimum width of a popup that is wide enough to show all the combobox's items horizontally
   */
  public int getMinimumPopupWidth() {
    // Original preferred size of JComboBox is calculated as max of all pref sizes of combobox's items.
    // See javax.swing.plaf.basic.BasicComboBoxUI#getDisplaySize()
    //
    // Please note that "getPreferredSize().width" cannot be used as getPreferredSize might be overridden
    // to return a value different to "max of all pref sizes of comboBox's items".
    return super.getPreferredSize().width;
  }

  private class AdjustingListCellRenderer implements ListCellRenderer {
    private final ListCellRenderer myOldRenderer;
    private final ComboBoxWithWidePopup myComboBox;

    public AdjustingListCellRenderer(ComboBoxWithWidePopup comboBox, ListCellRenderer oldRenderer) {
      myComboBox = comboBox;
      myOldRenderer = oldRenderer;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Object _value = value;
      if (index == -1 && _value instanceof String && !myComboBox.isValid()) {
        int minLength = getMinLength();

        Dimension size = myComboBox._getSuperSize();
        String stringValue = (String)_value;

        if (size.width == 0) {
          if (stringValue.length() > minLength) {
            _value = stringValue.substring(0, minLength);
          }
        }
      }

      return myOldRenderer.getListCellRendererComponent(list, _value, index, isSelected, cellHasFocus);
    }
  }
}
