/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui.components;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.util.NotNullFunction;
import com.intellij.util.ui.*;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author Anton Makeev
 * @author Konstantin Bulenkov
 */
public class JBList<E> extends JList<E> implements ComponentWithEmptyText, ComponentWithExpandableItems<Integer> {
  @Nonnull
  private StatusText myEmptyText;
  @Nonnull
  private ExpandableItemsHandler<Integer> myExpandableItemsHandler;

  @Nullable
  private AsyncProcessIcon myBusyIcon;
  private boolean myBusy;


  public JBList() {
    init();
  }

  public JBList(@Nonnull ListModel<E> dataModel) {
    super(dataModel);
    init();
  }

  @SafeVarargs
  public JBList(@Nonnull E... listData) {
    super(createDefaultListModel(listData));
    init();
  }

  @Nonnull
  @SafeVarargs
  public static <E> DefaultListModel<E> createDefaultListModel(@Nonnull E... items) {
    final DefaultListModel<E> model = new DefaultListModel<>();
    for (E item : items) {
      model.add(model.getSize(), item);
    }
    return model;
  }

  @Nonnull
  public static <E> DefaultListModel<E> createDefaultListModel(@Nonnull Iterable<? extends E> items) {
    final DefaultListModel<E> model = new DefaultListModel<>();
    for (E item : items) {
      model.add(model.getSize(), item);
    }
    return model;
  }

  public JBList(@Nonnull Collection<? extends E> items) {
    this(JBList.<E>createDefaultListModel(items));
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    if (!ScreenUtil.isStandardAddRemoveNotify(this)) return;

    if (myBusyIcon != null) {
      remove(myBusyIcon);
      Disposer.dispose(myBusyIcon);
      myBusyIcon = null;
    }
  }

  @Override
  public void doLayout() {
    super.doLayout();

    if (myBusyIcon != null) {
      myBusyIcon.updateLocation(this);
    }
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (myBusyIcon != null) {
      myBusyIcon.updateLocation(this);
    }
  }

  public void setPaintBusy(boolean paintBusy) {
    if (myBusy == paintBusy) return;

    myBusy = paintBusy;
    updateBusy();
  }

  private void updateBusy() {
    if (myBusy) {
      if (myBusyIcon == null) {
        myBusyIcon = new AsyncProcessIcon(toString()).setUseMask(false);
        myBusyIcon.setOpaque(false);
        myBusyIcon.setPaintPassiveIcon(false);
        add(myBusyIcon);
      }
    }

    if (myBusyIcon != null) {
      if (myBusy) {
        myBusyIcon.resume();
      }
      else {
        myBusyIcon.suspend();
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (myBusyIcon != null) {
              repaint();
            }
          }
        });
      }
      if (myBusyIcon != null) {
        myBusyIcon.updateLocation(this);
      }
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    myEmptyText.paint(this, g);
  }

  @Override
  public Dimension getPreferredSize() {
    if (getModel().getSize() == 0 && !StringUtil.isEmpty(getEmptyText().getText())) {
      Dimension s = getEmptyText().getPreferredSize();
      JBInsets.addTo(s, getInsets());
      return s;
    }
    else {
      return super.getPreferredSize();
    }
  }

  private void init() {
    setSelectionBackground(UIUtil.getListSelectionBackground());
    setSelectionForeground(UIUtil.getListSelectionForeground());

    myEmptyText = new StatusText(this) {
      @Override
      protected boolean isStatusVisible() {
        return JBList.this.isEmpty();
      }
    };

    myExpandableItemsHandler = createExpandableItemsHandler();
    setCellRenderer(new DefaultListCellRenderer());
  }

  public boolean isEmpty() {
    return getItemsCount() == 0;
  }

  public int getItemsCount() {
    ListModel model = getModel();
    return model == null ? 0 : model.getSize();
  }

  @Nonnull
  @Override
  public StatusText getEmptyText() {
    return myEmptyText;
  }

  public void setEmptyText(@Nonnull String text) {
    myEmptyText.setText(text);
  }

  @Override
  @Nonnull
  public ExpandableItemsHandler<Integer> getExpandableItemsHandler() {
    return myExpandableItemsHandler;
  }

  @Nonnull
  protected ExpandableItemsHandler<Integer> createExpandableItemsHandler() {
    return ExpandableItemsHandlerFactory.install(this);
  }

  @Override
  public void setExpandableItemsEnabled(boolean enabled) {
    myExpandableItemsHandler.setEnabled(enabled);
  }

  @Override
  public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
    // myExpandableItemsHandler may not yeb be initialized
    //noinspection ConstantConditions
    if (myExpandableItemsHandler == null) {
      super.setCellRenderer(cellRenderer);
      return;
    }
    super.setCellRenderer(new ExpandedItemListCellRendererWrapper<>(cellRenderer, myExpandableItemsHandler));
  }

  public <T> void installCellRenderer(@Nonnull final NotNullFunction<T, JComponent> fun) {
    setCellRenderer(new DefaultListCellRenderer() {
      @Nonnull
      @Override
      public Component getListCellRendererComponent(@Nonnull JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        @SuppressWarnings({"unchecked"})
        final JComponent comp = fun.fun((T)value);
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
    });
  }

  public void setDataProvider(@Nonnull DataProvider provider) {
    DataManager.registerDataProvider(this, provider);
  }

  public void disableEmptyText() {
    getEmptyText().setText("");
  }

  public static class StripedListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (!isSelected && index % 2 == 0) {
        setBackground(UIUtil.getDecoratedRowColor());
      }
      return this;
    }
  }
}
