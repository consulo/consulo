package consulo.execution.coverage.view;

import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.project.Project;
import consulo.project.ui.view.commander.AbstractListBuilder;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.SortableColumnModel;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * User: anna
 * Date: 1/3/12
 */
class CoverageTableModel extends AbstractTableModel implements AbstractListBuilder.Model, SortableColumnModel {
  private final ColumnInfo[] COLUMN_INFOS;

  final List myElements = new ArrayList();

  public CoverageTableModel(@Nonnull CoverageSuitesBundle suitesBundle, CoverageViewManager.StateBean stateBean, Project project) {
    final CoverageEngine coverageEngine = suitesBundle.getCoverageEngine();
    COLUMN_INFOS = coverageEngine.createCoverageViewExtension(project, suitesBundle, stateBean).createColumnInfos();
  }

  @Override
  public void removeAllElements() {
    myElements.clear();
    fireTableDataChanged();
  }

  @Override
  public void addElement(final Object obj) {
    myElements.add(obj);
    fireTableDataChanged();
  }

  @Override
  public void replaceElements(final List newElements) {
    removeAllElements();
    myElements.addAll(newElements);
    fireTableDataChanged();
  }

  @Override
  public Object[] toArray() {
    return ArrayUtil.toObjectArray(myElements);
  }

  @Override
  public int indexOf(final Object o) {
    return myElements.indexOf(o);
  }

  @Override
  public int getSize() {
    return myElements.size();
  }

  @Override
  public Object getElementAt(final int index) {
    return myElements.get(index);
  }

  @Override
  public int getRowCount() {
    return myElements.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMN_INFOS.length;
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_INFOS[column].getName();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    final Object element = getElementAt(rowIndex);
    if (columnIndex == 0) {
      return element;
    }
    else if (element instanceof CoverageListNode) {
      return COLUMN_INFOS[columnIndex].valueOf(element);
    }
    return element;
  }

  @Override
  public ColumnInfo[] getColumnInfos() {
    return COLUMN_INFOS;
  }

  @Override
  public void setSortable(boolean aBoolean) {
  }

  @Override
  public boolean isSortable() {
    return true;
  }

  @Override
  public Object getRowValue(int row) {
    return getElementAt(row);
  }

  @Override
  public RowSorter.SortKey getDefaultSortKey() {
    return null;
  }
}
