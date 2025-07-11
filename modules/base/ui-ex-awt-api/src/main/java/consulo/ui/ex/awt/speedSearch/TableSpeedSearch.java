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

package consulo.ui.ex.awt.speedSearch;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.Cell;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

public class TableSpeedSearch extends SpeedSearchBase<JTable> {
  private static final BiFunction<Object, Cell, String> TO_STRING = (o, cell) -> o == null || o instanceof Boolean ? "" : o.toString();
  private final BiFunction<Object, ? super Cell, String> myToStringConvertor;

  public TableSpeedSearch(JTable table) {
    this(table, TO_STRING);
  }

  public TableSpeedSearch(JTable table, final Function<Object, String> toStringConvertor) {
    this(table, (o, c) -> toStringConvertor.apply(o));
  }

  public TableSpeedSearch(JTable table, final BiFunction<Object, ? super Cell, String> toStringConvertor) {
    super(table);

    myToStringConvertor = toStringConvertor;
    // edit on F2 & double click, do not interfere with quick search
    table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

    new MySelectAllAction(table, this).registerCustomShortcutSet(table, null);
  }

  @Override
  protected boolean isSpeedSearchEnabled() {
    boolean tableIsNotEmpty = myComponent.getRowCount() != 0 && myComponent.getColumnCount() != 0;
    return tableIsNotEmpty && !myComponent.isEditing() && super.isSpeedSearchEnabled();
  }

  @Nonnull
  @Override
  protected ListIterator<Object> getElementIterator(int startingIndex) {
    return new MyListIterator(startingIndex);
  }

  @Override
  protected int getElementCount() {
    return myComponent.getRowCount() * myComponent.getColumnCount();
  }

  @Override
  protected void selectElement(Object element, String selectedText) {
    if (element instanceof Integer) {
      final int index = ((Integer)element).intValue();
      final int row = index / myComponent.getColumnCount();
      final int col = index % myComponent.getColumnCount();
      myComponent.getSelectionModel().setSelectionInterval(row, row);
      myComponent.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
      TableUtil.scrollSelectionToVisible(myComponent);
    }
    else {
      myComponent.getSelectionModel().clearSelection();
      myComponent.getColumnModel().getSelectionModel().clearSelection();
    }
  }

  @Override
  protected int getSelectedIndex() {
    final int row = myComponent.getSelectedRow();
    final int col = myComponent.getSelectedColumn();
    // selected row is not enough as we want to select specific cell in a large multi-column table
    return row > -1 && col > -1 ? row * myComponent.getColumnCount() + col : -1;
  }

  @Nonnull
  @Override
  protected Object[] getAllElements() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  protected String getElementText(Object element) {
    final int index = ((Integer)element).intValue();
    int row = index / myComponent.getColumnCount();
    int col = index % myComponent.getColumnCount();
    Object value = myComponent.getValueAt(row, col);
    return myToStringConvertor.apply(value, new Cell(row, col));
  }

  private class MyListIterator implements ListIterator<Object> {

    private int myCursor;

    MyListIterator(int startingIndex) {
      final int total = getElementCount();
      myCursor = startingIndex < 0 ? total : startingIndex;
    }

    @Override
    public boolean hasNext() {
      return myCursor < getElementCount();
    }

    @Override
    public Object next() {
      return myCursor++;
    }

    @Override
    public boolean hasPrevious() {
      return myCursor > 0;
    }

    @Override
    public Object previous() {
      return (myCursor--) - 1;
    }

    @Override
    public int nextIndex() {
      return myCursor;
    }

    @Override
    public int previousIndex() {
      return myCursor - 1;
    }

    @Override
    public void remove() {
      throw new AssertionError("Not Implemented");
    }

    @Override
    public void set(Object o) {
      throw new AssertionError("Not Implemented");
    }

    @Override
    public void add(Object o) {
      throw new AssertionError("Not Implemented");
    }
  }

  @Nonnull
  private IntList findAllFilteredRows(String s) {
    IntList rows = IntLists.newArrayList();
    String _s = s.trim();

    for (int row = 0; row < myComponent.getRowCount(); row++) {
      for (int col = 0; col < myComponent.getColumnCount(); col++) {
        int index = row * myComponent.getColumnCount() + col;
        if (isMatchingElement(index, _s)) {
          rows.add(row);
          break;
        }
      }
    }
    return rows;
  }

  private static class MySelectAllAction extends DumbAwareAction {
    @Nonnull
    private final JTable myTable;
    @Nonnull
    private final TableSpeedSearch mySearch;

    MySelectAllAction(@Nonnull JTable table, @Nonnull TableSpeedSearch search) {
      myTable = table;
      mySearch = search;
      copyShortcutFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_SELECT_ALL));
      setEnabledInModalContext(true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(
        mySearch.isPopupActive()
          && myTable.getRowSelectionAllowed()
          && myTable.getSelectionModel().getSelectionMode() == MULTIPLE_INTERVAL_SELECTION
      );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      ListSelectionModel sm = myTable.getSelectionModel();

      String query = mySearch.getEnteredPrefix();
      if (query == null) return;

      IntList filtered = mySearch.findAllFilteredRows(query);
      if (filtered.isEmpty()) return;

      boolean alreadySelected = Arrays.equals(filtered.toArray(), myTable.getSelectedRows());

      if (alreadySelected) {
        int anchor = sm.getAnchorSelectionIndex();

        sm.setSelectionInterval(anchor, anchor);
        sm.setAnchorSelectionIndex(anchor);

        mySearch.findAndSelectElement(query);
      }
      else {
        int anchor = -1;
        Object currentElement = mySearch.findElement(query);
        if (currentElement instanceof Integer) {
          int index = (Integer)currentElement;
          anchor = index / myTable.getColumnCount();
        }
        if (anchor == -1) anchor = filtered.get(0);

        sm.clearSelection();
        for (int i = 0; i < filtered.size(); i++) {
          int value = filtered.get(i);
          sm.addSelectionInterval(value, value);
        }
        sm.setAnchorSelectionIndex(anchor);
      }
    }
  }
}
