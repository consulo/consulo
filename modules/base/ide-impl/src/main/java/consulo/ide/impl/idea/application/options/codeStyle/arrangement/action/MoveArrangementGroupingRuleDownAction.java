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
package consulo.ide.impl.idea.application.options.codeStyle.arrangement.action;

import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementGroupingRulesControl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

import javax.swing.table.DefaultTableModel;

/**
 * @author Denis Zhdanov
 * @since 2012-11-14
 */
public class MoveArrangementGroupingRuleDownAction extends AnAction implements DumbAware {

  public MoveArrangementGroupingRuleDownAction() {
    super(
        ApplicationLocalize.arrangementActionRuleMoveDownText(),
        ApplicationLocalize.arrangementActionRuleMoveDownDescription()
    );
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ArrangementGroupingRulesControl control = e.getData(ArrangementGroupingRulesControl.KEY);
    if (control == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    int[] rows = control.getSelectedRows();
    e.getPresentation().setEnabled(rows.length == 1 && rows[0] != control.getRowCount() - 1);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    ArrangementGroupingRulesControl control = e.getData(ArrangementGroupingRulesControl.KEY);
    if (control == null) {
      return;
    }

    int[] rows = control.getSelectedRows();
    int row = rows[0];
    if (rows.length != 1 || rows[0] == control.getRowCount() - 1) {
      return;
    }

    if (control.isEditing()) {
      control.getCellEditor().stopCellEditing();
    }

    DefaultTableModel model = control.getModel();
    Object value = model.getValueAt(row, 0);
    model.removeRow(row);
    model.insertRow(row + 1, new Object[] { value });
    control.getSelectionModel().setSelectionInterval(row + 1, row + 1);
  }
}
