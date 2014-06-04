/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.ide.impl.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class DListWithChildren extends JBList {
  public class MyModel extends AbstractListModel {
    @Override
    public int getSize() {
      if (mySelected == null) {
        return 0;
      }
      DListItem parent = mySelected.getParent();
      List<DListItem> items = mySelected.getItems();
      return parent == null ? items.size() : items.size() + 1;
    }

    @Override
    public Object getElementAt(int index) {
      DListItem parent = mySelected.getParent();
      List<DListItem> items = mySelected.getItems();
      if (parent == null) {
        return items.get(index);
      }
      else {
        if (items.size() == index) {
          return ourReturnItem;
        }
        else {
          return items.get(index);
        }
      }
    }

    @Override
    public void fireContentsChanged(Object source, int index0, int index1) {
      super.fireContentsChanged(source, index0, index1);
    }
  }

  private static DListItem ourReturnItem = DListItem.builder().withName("Back").withIcon(AllIcons.Actions.Back).create();

  private Consumer<DListItem> myConsumer;
  private DListItem mySelected;

  public DListWithChildren() {
    setModel(new MyModel());
    setCellRenderer(new ColoredListCellRenderer<DListItem>() {
      @Override
      protected void customizeCellRenderer(JList list, DListItem value, int index, boolean selected, boolean hasFocus) {
        SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        if (value == ourReturnItem) {
          attributes = SimpleTextAttributes.GRAYED_ATTRIBUTES;
        }
        else if (!value.getItems().isEmpty()) {
          attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        }

        String name = value.getName();
        assert name != null;
        append(name, attributes);

        setIcon(value.getIcon());
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        DListItem selectedValue = (DListItem)getSelectedValue();

        DListItem newSelectValue = null;
        if (selectedValue == ourReturnItem) {
          newSelectValue = mySelected.getParent();
        }
        else if (!selectedValue.getItems().isEmpty()) {
          newSelectValue = selectedValue;
        }

        if (newSelectValue != null) {
          select(newSelectValue);
        }

        return true;
      }
    }.installOn(this);

    addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        DListItem selectedValue = (DListItem)getSelectedValue();
        if(!selectedValue.getItems().isEmpty() || selectedValue == ourReturnItem) {
          selectedValue = null;
        }
        if (myConsumer != null) {
          myConsumer.consume(selectedValue);
        }
      }
    });
  }

  public void setConsumer(@NotNull Consumer<DListItem> consumer) {
    myConsumer = consumer;
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Font font = getFont();
    setFont(font.deriveFont(font.getStyle(), font.getSize() + 2));
  }

  public void select(@NotNull DListItem root) {
    mySelected = root;

    MyModel model = (MyModel)getModel();
    model.fireContentsChanged(this, -1, -1);

    setSelectedValue(ourReturnItem, false);
  }
}
