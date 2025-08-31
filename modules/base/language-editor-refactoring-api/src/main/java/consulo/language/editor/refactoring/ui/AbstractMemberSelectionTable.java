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

package consulo.language.editor.refactoring.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.Iconable;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.language.editor.refactoring.classMember.MemberInfoBase;
import consulo.language.editor.refactoring.classMember.MemberInfoChange;
import consulo.language.editor.refactoring.classMember.MemberInfoChangeListener;
import consulo.language.editor.refactoring.classMember.MemberInfoModel;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public abstract class AbstractMemberSelectionTable<T extends PsiElement, M extends MemberInfoBase<T>> extends JBTable implements TypeSafeDataProvider {
  protected static final int CHECKED_COLUMN = 0;
  protected static final int DISPLAY_NAME_COLUMN = 1;
  protected static final int ABSTRACT_COLUMN = 2;
  public static final Image EMPTY_OVERRIDE_ICON = Image.empty(Image.DEFAULT_ICON_SIZE);
  protected static final int OVERRIDE_ICON_POSITION = 2;
  protected static final int VISIBILITY_ICON_POSITION = 1;
  protected static final int MEMBER_ICON_POSITION = 0;

  protected final String myAbstractColumnHeader;
  protected List<M> myMemberInfos;
  protected final boolean myAbstractEnabled;
  protected MemberInfoModel<T, M> myMemberInfoModel;
  protected MyTableModel<T, M> myTableModel;

  public AbstractMemberSelectionTable(Collection<M> memberInfos, @Nullable MemberInfoModel<T, M> memberInfoModel, @Nullable String abstractColumnHeader) {
    myAbstractEnabled = abstractColumnHeader != null;
    myAbstractColumnHeader = abstractColumnHeader;
    myTableModel = new MyTableModel<>(this);

    myMemberInfos = new ArrayList<>(memberInfos);
    if (memberInfoModel != null) {
      myMemberInfoModel = memberInfoModel;
    }
    else {
      myMemberInfoModel = new DefaultMemberInfoModel<>();
    }

    setModel(myTableModel);

    TableColumnModel model = getColumnModel();
    model.getColumn(DISPLAY_NAME_COLUMN).setCellRenderer(new MyTableRenderer<>(this));
    TableColumn checkBoxColumn = model.getColumn(CHECKED_COLUMN);
    TableUtil.setupCheckboxColumn(checkBoxColumn);
    checkBoxColumn.setCellRenderer(new MyBooleanRenderer<>(this));
    if (myAbstractEnabled) {
      int width = (int)(1.3 * getFontMetrics(getFont()).charsWidth(myAbstractColumnHeader.toCharArray(), 0, myAbstractColumnHeader.length()));
      model.getColumn(ABSTRACT_COLUMN).setMaxWidth(width);
      model.getColumn(ABSTRACT_COLUMN).setPreferredWidth(width);
      model.getColumn(ABSTRACT_COLUMN).setCellRenderer(new MyBooleanRenderer<>(this));
    }

    setPreferredScrollableViewportSize(new Dimension(400, getRowHeight() * 12));
    getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));

    new MyEnableDisableAction().register();
    new TableSpeedSearch(this);
  }

  public Collection<M> getSelectedMemberInfos() {
    ArrayList<M> list = new ArrayList<>(myMemberInfos.size());
    for (M info : myMemberInfos) {
      if (isMemberInfoSelected(info)) {
//      if (info.isChecked() || (!myMemberInfoModel.isMemberEnabled(info) && myMemberInfoModel.isCheckedWhenDisabled(info))) {
        list.add(info);
      }
    }
    return list;
  }

  private boolean isMemberInfoSelected(M info) {
    boolean memberEnabled = myMemberInfoModel.isMemberEnabled(info);
    return (memberEnabled && info.isChecked()) || (!memberEnabled && myMemberInfoModel.isCheckedWhenDisabled(info));
  }

  public MemberInfoModel<T, M> getMemberInfoModel() {
    return myMemberInfoModel;
  }

  public void setMemberInfoModel(MemberInfoModel<T, M> memberInfoModel) {
    myMemberInfoModel = memberInfoModel;
  }

  public void fireExternalDataChange() {
    myTableModel.fireTableDataChanged();
  }

  /**
   * Redraws table
   */
  public void redraw() {
    myTableModel.redraw(getSelectedMemberInfos());
    myTableModel.fireTableDataChanged();
  }

  public void setMemberInfos(Collection<M> memberInfos) {
    myMemberInfos = new ArrayList<>(memberInfos);
    fireMemberInfoChange(memberInfos);
    myTableModel.fireTableDataChanged();
  }

  public void addMemberInfoChangeListener(MemberInfoChangeListener<T, M> l) {
    listenerList.add(MemberInfoChangeListener.class, l);
  }

  protected void fireMemberInfoChange(Collection<M> changedMembers) {
    Object[] list = listenerList.getListenerList();

    MemberInfoChange<T, M> event = new MemberInfoChange<>(changedMembers);
    for (Object element : list) {
      if (element instanceof MemberInfoChangeListener) {
        @SuppressWarnings("unchecked") MemberInfoChangeListener<T, M> changeListener = (MemberInfoChangeListener<T, M>)element;
        changeListener.memberInfoChanged(event);
      }
    }
  }

  @Override
  public void calcData(Key key, DataSink sink) {
    if (key == PsiElement.KEY) {
      Collection<M> memberInfos = getSelectedMemberInfos();
      if (memberInfos.size() > 0) {
        sink.put(PsiElement.KEY, memberInfos.iterator().next().getMember());
      }
    }
  }

  public void scrollSelectionInView() {
    for (int i = 0; i < myMemberInfos.size(); i++) {
      if (isMemberInfoSelected(myMemberInfos.get(i))) {
        Rectangle rc = getCellRect(i, 0, false);
        scrollRectToVisible(rc);
        break;
      }
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    scrollSelectionInView();
  }

  @Nullable
  protected abstract Object getAbstractColumnValue(M memberInfo);

  protected abstract boolean isAbstractColumnEditable(int rowIndex);

  protected abstract Image getOverrideIcon(M memberInfo);

  private static class DefaultMemberInfoModel<T extends PsiElement, M extends MemberInfoBase<T>> implements MemberInfoModel<T, M> {
    @Override
    public boolean isMemberEnabled(M member) {
      return true;
    }

    @Override
    public boolean isCheckedWhenDisabled(M member) {
      return false;
    }

    @Override
    public boolean isAbstractEnabled(M member) {
      return true;
    }

    @Override
    public boolean isAbstractWhenDisabled(M member) {
      return false;
    }


    @Override
    public int checkForProblems(@Nonnull M member) {
      return OK;
    }

    @Override
    public void memberInfoChanged(MemberInfoChange<T, M> event) {
    }

    @Override
    public Boolean isFixedAbstract(M member) {
      return null;
    }

    @Override
    public String getTooltipText(M member) {
      return null;
    }
  }

  protected static class MyTableModel<T extends PsiElement, M extends MemberInfoBase<T>> extends AbstractTableModel {
    private final AbstractMemberSelectionTable<T, M> myTable;

    public MyTableModel(AbstractMemberSelectionTable<T, M> table) {
      myTable = table;
    }

    @Override
    public int getColumnCount() {
      if (myTable.myAbstractEnabled) {
        return 3;
      }
      else {
        return 2;
      }
    }

    @Override
    public int getRowCount() {
      return myTable.myMemberInfos.size();
    }

    @Override
    public Class getColumnClass(int columnIndex) {
      if (columnIndex == CHECKED_COLUMN || columnIndex == ABSTRACT_COLUMN) {
        return Boolean.class;
      }
      return super.getColumnClass(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      M memberInfo = myTable.myMemberInfos.get(rowIndex);
      switch (columnIndex) {
        case CHECKED_COLUMN:
          if (myTable.myMemberInfoModel.isMemberEnabled(memberInfo)) {
            return memberInfo.isChecked();
          }
          else {
            return myTable.myMemberInfoModel.isCheckedWhenDisabled(memberInfo);
          }
        case ABSTRACT_COLUMN: {
          return myTable.getAbstractColumnValue(memberInfo);
        }
        case DISPLAY_NAME_COLUMN:
          return memberInfo.getDisplayName();
        default:
          throw new RuntimeException("Incorrect column index");
      }
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case CHECKED_COLUMN:
          return " ";
        case ABSTRACT_COLUMN:
          return myTable.myAbstractColumnHeader;
        case DISPLAY_NAME_COLUMN:
          return RefactoringLocalize.memberColumn().get();
        default:
          throw new RuntimeException("Incorrect column index");
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      switch (columnIndex) {
        case CHECKED_COLUMN:
          return myTable.myMemberInfoModel.isMemberEnabled(myTable.myMemberInfos.get(rowIndex));
        case ABSTRACT_COLUMN:
          return myTable.isAbstractColumnEditable(rowIndex);
      }
      return false;
    }


    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == CHECKED_COLUMN) {
        myTable.myMemberInfos.get(rowIndex).setChecked((Boolean)aValue);
      }
      else if (columnIndex == ABSTRACT_COLUMN) {
        myTable.myMemberInfos.get(rowIndex).setToAbstract((Boolean)aValue);
      }

      Collection<M> changed = Collections.singletonList(myTable.myMemberInfos.get(rowIndex));
      redraw(changed);
//      fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void redraw(Collection<M> changed) {
      myTable.fireMemberInfoChange(changed);
      fireTableDataChanged();
    }
  }

  private class MyEnableDisableAction extends EnableDisableAction {

    @Override
    protected JTable getTable() {
      return AbstractMemberSelectionTable.this;
    }

    @Override
    protected void applyValue(int[] rows, boolean valueToBeSet) {
      List<M> changedInfo = new ArrayList<>();
      for (int row : rows) {
        M memberInfo = myMemberInfos.get(row);
        memberInfo.setChecked(valueToBeSet);
        changedInfo.add(memberInfo);
      }
      fireMemberInfoChange(changedInfo);
      int[] selectedRows = getSelectedRows();
      myTableModel.fireTableDataChanged();
      ListSelectionModel selectionModel = getSelectionModel();
      for (int selectedRow : selectedRows) {
        selectionModel.addSelectionInterval(selectedRow, selectedRow);
      }
    }

    @Override
    protected boolean isRowChecked(int row) {
      return myMemberInfos.get(row).isChecked();
    }
  }

  private static class MyTableRenderer<T extends PsiElement, M extends MemberInfoBase<T>> extends ColoredTableCellRenderer {
    private final AbstractMemberSelectionTable<T, M> myTable;

    public MyTableRenderer(AbstractMemberSelectionTable<T, M> table) {
      myTable = table;
    }

    @Override
    @RequiredReadAction
    public void customizeCellRenderer(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      int modelColumn = myTable.convertColumnIndexToModel(column);
      M memberInfo = myTable.myMemberInfos.get(row);
      setToolTipText(myTable.myMemberInfoModel.getTooltipText(memberInfo));
      switch (modelColumn) {
        case DISPLAY_NAME_COLUMN: {
          Image memberIcon = myTable.getMemberIcon(memberInfo, 0);
          Image overrideIcon = myTable.getOverrideIcon(memberInfo);

          setIcon(ImageEffects.appendRight(ImageEffects.appendRight(memberIcon, Image.empty(Image.DEFAULT_ICON_SIZE)), overrideIcon));
          break;
        }
        default: {
          setIcon(null);
        }
      }
      setIconOpaque(false);
      setOpaque(false);
      boolean cellEditable = myTable.myMemberInfoModel.isMemberEnabled(memberInfo);
      setEnabled(cellEditable);

      if (value == null) return;
      int problem = myTable.myMemberInfoModel.checkForProblems(memberInfo);
      Color c = null;
      if (problem == MemberInfoModel.ERROR) {
        c = JBColor.RED;
      }
      else if (problem == MemberInfoModel.WARNING && !isSelected) {
        c = JBColor.BLUE;
      }
      append((String)value, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, c));
    }

  }

  @RequiredReadAction
  protected Image getMemberIcon(M memberInfo, @Iconable.IconFlags int flags) {
    return IconDescriptorUpdaters.getIcon(memberInfo.getMember(), flags);
  }

  private static class MyBooleanRenderer<T extends PsiElement, M extends MemberInfoBase<T>> extends BooleanTableCellRenderer {
    private final AbstractMemberSelectionTable<T, M> myTable;

    public MyBooleanRenderer(AbstractMemberSelectionTable<T, M> table) {
      myTable = table;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (component instanceof JCheckBox) {
        int modelColumn = myTable.convertColumnIndexToModel(column);
        M memberInfo = myTable.myMemberInfos.get(row);
        component.setEnabled((modelColumn == CHECKED_COLUMN && myTable.myMemberInfoModel.isMemberEnabled(memberInfo)) ||
                             (modelColumn == ABSTRACT_COLUMN && memberInfo.isChecked() && myTable.isAbstractColumnEditable(row)));
      }
      return component;
    }
  }
}
