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

package consulo.ide.impl.idea.ide.todo.configurable;

import consulo.application.HelpManager;
import consulo.language.editor.todo.TodoFilter;
import consulo.ide.impl.idea.util.ui.Table;
import consulo.language.psi.search.TodoPattern;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ScrollPaneFactory;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
class FilterDialog extends DialogWrapper {
  private final TodoFilter myFilter;
  private final int myFilterIndex;
  private final List<TodoPattern> myPatterns;
  private final List<TodoFilter> myFilters;

  private final JTextField myNameField;
  private final Table myTable;

  /**
   * @param parent      parent component.
   * @param filter      filter to be edited.
   * @param filterIndex index of <code>filter</code> in the <code>filters</code>. This parameter is
   *                    needed to not compare filter with itself when validating.
   * @param filters     all already configured filters. This parameter is used to
   * @param patterns    all patterns available in this filter.
   */
  public FilterDialog(
    Component parent,
    TodoFilter filter,
    int filterIndex,
    List<TodoFilter> filters,
    List<TodoPattern> patterns
  ) {
    super(parent, true);
    myFilter = filter;
    myFilterIndex = filterIndex;
    myPatterns = patterns;
    myFilters = filters;
    myNameField = new JTextField(filter.getName());
    MyModel model = new MyModel();
    myTable = new Table(model);
    init();
  }

  @Override
  protected void doOKAction() {

    // Validate filter name

    myFilter.setName(myNameField.getText().trim());
    if (myFilter.getName().length() == 0) {
      Messages.showMessageDialog(
        myTable,
        IdeLocalize.errorFilterNameShouldBeSpecified().get(),
        CommonLocalize.titleError().get(),
        Messages.getErrorIcon()
      );
      return;
    }
    for (int i = 0; i < myFilters.size(); i++) {
      TodoFilter filter = myFilters.get(i);
      if (myFilterIndex != i && myFilter.getName().equals(filter.getName())) {
        Messages.showMessageDialog(
          myTable,
          IdeLocalize.errorFilterWithTheSameNameAlreadyExists().get(),
          CommonLocalize.titleError().get(),
          Messages.getErrorIcon()
        );
        return;
      }
    }

    // Validate that at least one pettern is selected

    if (myFilter.isEmpty()) {
      Messages.showMessageDialog(
        myTable,
        IdeLocalize.errorFilterShouldContainAtLeastOnePattern().get(),
        CommonLocalize.titleError().get(),
        Messages.getErrorIcon()
      );
      return;
    }

    super.doOKAction();
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp("reference.idesettings.todo.editfilter");
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    JLabel nameLabel = new JLabel(IdeLocalize.labelTodoFilterName().get());
    panel.add(
      nameLabel,
      new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(0, 0, 5, 10), 0, 0
      )
    );
    panel.add(
      myNameField,
      new GridBagConstraints(
        1, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 5, 0), 0, 0
      )
    );

    JPanel patternsPanel = new JPanel(new GridBagLayout());
    Border border = IdeBorderFactory.createTitledBorder(IdeLocalize.groupTodoFilterPatterns().get(), false);
    patternsPanel.setBorder(border);
    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);
    scrollPane.setPreferredSize(new Dimension(550, myTable.getRowHeight() * 10));
    patternsPanel.add(
      scrollPane,
      new GridBagConstraints(
        0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0
      )
    );

    // Column "Available"

    int width = new JCheckBox().getPreferredSize().width;
    TableColumn availableColumn = myTable.getColumnModel().getColumn(0);
    availableColumn.setPreferredWidth(width);
    availableColumn.setMaxWidth(width);
    availableColumn.setMinWidth(width);

    //

    panel.add(
      patternsPanel,
      new GridBagConstraints(
        0, 1, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0
      )
    );

    return panel;
  }

  private final class MyModel extends AbstractTableModel {
    private final String[] ourColumnNames = new String[]{" ", IdeLocalize.columnTodoFilterPattern().get(), };
    private final Class[] ourColumnClasses = new Class[]{Boolean.class, String.class};

    @Override
    public String getColumnName(int column) {
      return ourColumnNames[column];
    }

    @Override
    public Class getColumnClass(int column) {
      return ourColumnClasses[column];
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public int getRowCount() {
      return myPatterns.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
      TodoPattern pattern = myPatterns.get(row);
      switch (column) {
        case 0:
          // "Available" column
          return myFilter.contains(pattern) ? Boolean.TRUE : Boolean.FALSE;
        case 1:
          // "Pattern" column
          return pattern.getPatternString();
        default:
          throw new IllegalArgumentException();
      }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
      switch (column) {
        case 0:
          TodoPattern pattern = myPatterns.get(row);
          if ((Boolean)value) {
            myFilter.addTodoPattern(pattern);
          }
          else {
            myFilter.removeTodoPattern(pattern);
          }
          break;
        default:
          throw new IllegalArgumentException();
      }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return column == 0;
    }
  }
}
