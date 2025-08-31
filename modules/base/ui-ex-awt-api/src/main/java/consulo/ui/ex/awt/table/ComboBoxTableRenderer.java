/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ui.ex.awt.table;

import consulo.application.AllIcons;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.EventObject;
import java.util.List;

/**
 * @author spleaner
 */
public class ComboBoxTableRenderer<T> extends JLabel implements TableCellRenderer, TableCellEditor, JBPopupListener {

  private final T[] myValues;
  private WeakReference<ListPopup> myPopupRef;
  private ChangeEvent myChangeEvent = null;
  private T myValue;
  private boolean myPaintArrow = true;

  protected EventListenerList myListenerList = new EventListenerList();

  private Runnable myFinalRunnable;

  public ComboBoxTableRenderer(T[] values) {
    myValues = values;
    setFont(UIUtil.getButtonFont());
    setBorder(JBUI.Borders.empty(0, 5, 0, 5));
  }

  @Override
  public Dimension getPreferredSize() {
    return addIconSize(super.getPreferredSize());
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  private static Dimension addIconSize(Dimension d) {
    return new Dimension(d.width + AllIcons.General.ArrowDown.getWidth() + JBUI.scale(2),
                         Math.max(d.height, AllIcons.General.ArrowDown.getHeight()));
  }

  protected String getTextFor(@Nonnull T value) {
    return value.toString();
  }

  protected Image getIconFor(@Nonnull T value) {
    return null;
  }

  public void setPaintArrow(boolean paintArrow) {
    myPaintArrow = paintArrow;
  }

  protected Runnable onChosen(@Nonnull T value) {
    stopCellEditing(value);

    return () -> stopCellEditing(value);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (!StringUtil.isEmpty(getText()) && myPaintArrow) {
      Rectangle r = getBounds();
      Insets i = getInsets();
      int x = r.width - i.right - AllIcons.General.ArrowDown.getWidth();
      int y = i.top + (r.height - i.top - i.bottom - AllIcons.General.ArrowDown.getHeight()) / 2;
      TargetAWT.to(AllIcons.General.ArrowDown).paintIcon(this, g, x, y);
    }
  }

  @Override
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column) {
    @SuppressWarnings("unchecked") T t = (T)value;
    customizeComponent(t, table, isSelected);
    return this;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column) {
    @SuppressWarnings("unchecked") T t = (T)value;
    myValue = t;
    customizeComponent(t, table, true);

    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> showPopup(t, row));

    return this;
  }

  protected boolean isApplicable(T value, int row) {
    return true;
  }

  private void showPopup(final T value, int row) {
    List<T> filtered = ContainerUtil.findAll(myValues, t -> isApplicable(t, row));
    ListPopup popup = JBPopupFactory.getInstance().createListPopup(new ListStep<T>(filtered, value) {
      @Override
      @Nonnull
      public String getTextFor(T value) {
        return ComboBoxTableRenderer.this.getTextFor(value);
      }

      @Override
      public Image getIconFor(T value) {
        return ComboBoxTableRenderer.this.getIconFor(value);
      }

      @Override
      public PopupStep onChosen(T selectedValue, boolean finalChoice) {
        myFinalRunnable = ComboBoxTableRenderer.this.onChosen(selectedValue);
        return FINAL_CHOICE;
      }

      @Override
      public void canceled() {
        ComboBoxTableRenderer.this.cancelCellEditing();
      }

      @Override
      public Runnable getFinalRunnable() {
        return myFinalRunnable;
      }
    });

    popup.addListener(this);
    popup.setRequestFocus(false);

    myPopupRef = new WeakReference<>(popup);
    popup.showUnderneathOf(this);
  }

  @Override
  public void beforeShown(LightweightWindowEvent event) {
  }

  @Override
  public void onClosed(LightweightWindowEvent event) {
    event.asPopup().removeListener(this);
    fireEditingCanceled();
  }

  protected void customizeComponent(T value, JTable table, boolean isSelected) {
    setOpaque(true);
    setText(value == null ? "" : getTextFor(value));
    if (value != null) {
      setIcon(TargetAWT.to(getIconFor(value)));
    }
    setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
  }

  @Override
  public Object getCellEditorValue() {
    return myValue;
  }

  @Override
  public boolean isCellEditable(EventObject event) {
    if (event instanceof MouseEvent) {
      return ((MouseEvent)event).getClickCount() >= 2;
    }

    return true;
  }

  @Override
  public boolean shouldSelectCell(EventObject event) {
    return true;
  }

  private void stopCellEditing(T value) {
    myValue = value;
    stopCellEditing();
  }

  @Override
  public boolean stopCellEditing() {
    fireEditingStopped();
    hidePopup();
    return true;
  }

  @Override
  public void cancelCellEditing() {
    fireEditingCanceled();
    hidePopup();
  }

  protected void fireEditingStopped() {
    // Guaranteed to return a non-null array
    Object[] listeners = myListenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CellEditorListener.class) {
        // Lazily create the event:
        if (myChangeEvent == null) {
          myChangeEvent = new ChangeEvent(this);
        }
        ((CellEditorListener)listeners[i + 1]).editingStopped(myChangeEvent);
      }
    }
  }

  protected void fireEditingCanceled() {
    // Guaranteed to return a non-null array
    Object[] listeners = myListenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CellEditorListener.class) {
        // Lazily create the event:
        if (myChangeEvent == null) {
          myChangeEvent = new ChangeEvent(this);
        }
        ((CellEditorListener)listeners[i + 1]).editingCanceled(myChangeEvent);
      }
    }
  }

  private void hidePopup() {
    if (myPopupRef != null) {
      ListPopup popup = myPopupRef.get();
      if (popup != null && popup.isVisible()) {
        popup.cancel();
      }

      myPopupRef = null;
    }
  }

  @Override
  public void addCellEditorListener(CellEditorListener l) {
    myListenerList.add(CellEditorListener.class, l);
  }

  @Override
  public void removeCellEditorListener(CellEditorListener l) {
    myListenerList.remove(CellEditorListener.class, l);
  }

  private abstract static class ListStep<T> implements ListPopupStep<T> {
    private final List<T> myValues;
    private final T mySelected;

    protected ListStep(List<T> values, T selected) {
      myValues = values;
      mySelected = selected;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public boolean hasSubstep(T selectedValue) {
      return false;
    }

    @Override
    public boolean isMnemonicsNavigationEnabled() {
      return false;
    }

    @Override
    public boolean isSpeedSearchEnabled() {
      return false;
    }

    @Override
    public boolean isAutoSelectionEnabled() {
      return false;
    }

    @Override
    @Nonnull
    public List<T> getValues() {
      return myValues;
    }

    @Override
    public boolean isSelectable(T value) {
      return true;
    }

    @Override
    public Image getIconFor(T aValue) {
      return null;
    }

    @Override
    public ListSeparator getSeparatorAbove(T value) {
      return null;
    }

    @Override
    public int getDefaultOptionIndex() {
      return mySelected == null ? 0 : myValues.indexOf(mySelected);
    }

    @Override
    public MnemonicNavigationFilter<T> getMnemonicNavigationFilter() {
      return null;
    }

    @Override
    public SpeedSearchFilter<T> getSpeedSearchFilter() {
      return null;
    }
  }
}
