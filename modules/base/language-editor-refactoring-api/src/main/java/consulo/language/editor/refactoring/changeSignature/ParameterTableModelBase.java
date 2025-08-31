/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.changeSignature;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.CodeFragmentTableCellEditorBase;
import consulo.language.editor.refactoring.ui.CodeFragmentTableCellRenderer;
import consulo.language.editor.refactoring.ui.StringTableCellEditor;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.table.BooleanTableCellEditor;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.RowEditableTableModel;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ParameterTableModelBase<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>> extends ListTableModel<TableItem> implements RowEditableTableModel {

  protected final PsiElement myTypeContext;
  protected final PsiElement myDefaultValueContext;

  public ParameterTableModelBase(PsiElement typeContext,
                                 PsiElement defaultValueContext,
                                 ColumnInfo... columnInfos) {
    super(columnInfos);
    myTypeContext = typeContext;
    myDefaultValueContext = defaultValueContext;
  }

  protected abstract TableItem createRowItem(@Nullable P parameterInfo);

  @Override
  public void addRow() {
    addRow(createRowItem(null));
  }

  public void setParameterInfos(List<P> parameterInfos) {
    List<TableItem> items = new ArrayList<TableItem>(parameterInfos.size());
    for (P parameterInfo : parameterInfos) {
      items.add(createRowItem(parameterInfo));
    }
    setItems(items);
  }

  public void setValueAtWithoutUpdate(Object aValue, int rowIndex, int columnIndex) {
    super.setValueAt(aValue, rowIndex, columnIndex);
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    super.setValueAt(aValue, rowIndex, columnIndex);
    fireTableCellUpdated(rowIndex, columnIndex); // to update signature
  }

  protected static abstract class ColumnInfoBase<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>, Aspect>
    extends ColumnInfo<TableItem, Aspect> {
    private TableCellRenderer myRenderer;
    private TableCellEditor myEditor;

    public ColumnInfoBase(String name) {
      super(name);
    }

    @Override
    public final TableCellEditor getEditor(TableItem o) {
      if (myEditor == null) {
        myEditor = doCreateEditor(o);
      }
      return myEditor;
    }

    @Override
    public final TableCellRenderer getRenderer(TableItem item) {
      if (myRenderer == null) {
        final TableCellRenderer original = doCreateRenderer(item);
        myRenderer = new TableCellRenderer() {

          @Override
          public Component getTableCellRendererComponent(JTable table,
                                                         Object value,
                                                         boolean isSelected,
                                                         boolean hasFocus,
                                                         int row,
                                                         int column) {
            Component component = original.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!table.isCellEditable(row, table.convertColumnIndexToModel(column))) {
              Color bg = table.getBackground().darker();
              component.setBackground(ColorUtil.toAlpha(bg, 230));
            }

            if (component instanceof SimpleColoredComponent coloredComponent) {
              coloredComponent.setIpad(new Insets(0, 0, 0, 0));
            }

            return component;
          }
        };
      }
      return myRenderer;
    }

    protected abstract TableCellRenderer doCreateRenderer(TableItem item);

    protected abstract TableCellEditor doCreateEditor(TableItem item);
  }

  protected static class TypeColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>> extends ColumnInfoBase<P, TableItem, PsiCodeFragment> {
    protected final Project myProject;
    private final FileType myFileType;

    public TypeColumn(Project project, FileType fileType) {
      this(project, fileType, RefactoringLocalize.columnNameType().get());
    }

    public TypeColumn(Project project, FileType fileType, String title) {
      super(title);
      myProject = project;
      myFileType = fileType;
    }

    @Override
    public PsiCodeFragment valueOf(TableItem item) {
      return item.typeCodeFragment;
    }

    @Override
    public boolean isCellEditable(TableItem pParameterTableModelItemBase) {
      return true;
    }

    @Override
    public TableCellRenderer doCreateRenderer(TableItem pParameterTableModelItemBase) {
      return new CodeFragmentTableCellRenderer(myProject, myFileType);
    }

    @Override
    public TableCellEditor doCreateEditor(TableItem o) {
      return new CodeFragmentTableCellEditorBase(myProject, myFileType);
    }
  }

  protected static class NameColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>> extends ColumnInfoBase<P, TableItem, String> {
    private final Project myProject;

    public NameColumn(Project project) {
      this(project, RefactoringLocalize.columnNameName().get());
    }

    public NameColumn(Project project, String title) {
      super(title);
      myProject = project;
    }

    @Override
    public String valueOf(TableItem item) {
      return item.parameter.getName();
    }

    @Override
    public void setValue(TableItem item, String value) {
      item.parameter.setName(value);
    }

    @Override
    public boolean isCellEditable(TableItem pParameterTableModelItemBase) {
      return true;
    }

    @Override
    public TableCellRenderer doCreateRenderer(TableItem item) {
      return new ColoredTableCellRenderer() {
        @Override
        public void customizeCellRenderer(JTable table, Object value,
                                          boolean isSelected, boolean hasFocus, int row, int column) {
          if (value == null) return;
          append((String)value, new SimpleTextAttributes(Font.PLAIN, null));
        }
      };
    }

    @Override
    public TableCellEditor doCreateEditor(TableItem o) {
      return new StringTableCellEditor(myProject);
    }
  }

  protected static class DefaultValueColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>>
    extends ColumnInfoBase<P, TableItem, PsiCodeFragment> {
    private final Project myProject;
    private final FileType myFileType;

    public DefaultValueColumn(Project project, FileType fileType) {
      this(project, fileType, RefactoringLocalize.columnNameDefaultValue().get());
    }

    public DefaultValueColumn(Project project, FileType fileType, String title) {
      super(title);
      myProject = project;
      myFileType = fileType;
    }

    @Override
    public boolean isCellEditable(TableItem item) {
      return !item.isEllipsisType() && item.parameter.getOldIndex() == -1;
    }

    @Override
    public PsiCodeFragment valueOf(TableItem item) {
      return item.defaultValueCodeFragment;
    }

    @Override
    public TableCellRenderer doCreateRenderer(TableItem item) {
      return new CodeFragmentTableCellRenderer(myProject, myFileType);
    }

    @Override
    public TableCellEditor doCreateEditor(TableItem item) {
      return new CodeFragmentTableCellEditorBase(myProject, myFileType);
    }
  }

  protected static class AnyVarColumn<P extends ParameterInfo, TableItem extends ParameterTableModelItemBase<P>>
    extends ColumnInfoBase<P, TableItem, Boolean> {

    public AnyVarColumn() {
      super(RefactoringLocalize.columnNameAnyVar().get());
    }

    @Override
    public boolean isCellEditable(TableItem item) {
      return !item.isEllipsisType() && item.parameter.getOldIndex() == -1;
    }

    @Override
    public Boolean valueOf(TableItem item) {
      return item.parameter.isUseAnySingleVariable();
    }

    @Override
    public void setValue(TableItem item, Boolean value) {
      item.parameter.setUseAnySingleVariable(value);
    }

    @Override
    public TableCellRenderer doCreateRenderer(TableItem item) {
      return new BooleanTableCellRenderer();
    }

    @Override
    public TableCellEditor doCreateEditor(TableItem item) {
      return new BooleanTableCellEditor(false);
    }

    @Override
    public int getWidth(JTable table) {
      int headerWidth = table.getFontMetrics(table.getFont()).stringWidth(getName()) + 8;
      return Math.max(new JCheckBox().getPreferredSize().width, headerWidth);
    }
  }

}
