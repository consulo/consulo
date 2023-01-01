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

import consulo.language.codeStyle.ui.internal.arrangement.ArrangementMatchingRulesControl;
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementMatchingRulesModel;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationBundle;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.util.collection.primitive.ints.IntList;

/**
 * @author Denis Zhdanov
 * @since 8/26/12 7:41 PM
 */
public class RemoveArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware {

  public RemoveArrangementRuleAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.remove.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.remove.description"));
    getTemplatePresentation().setIcon(IconUtil.getRemoveIcon());
  }

  @Override
  public void update(AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    e.getPresentation().setEnabled(control != null && !control.getSelectedModelRows().isEmpty() && control.getEditingRow() == -1);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    if (control == null) {
      return;
    }

    control.hideEditor();

    final IntList rowsToRemove = control.getSelectedModelRows();
    if (rowsToRemove.isEmpty()) {
      return;
    }

    final ArrangementMatchingRulesModel model = control.getModel();
    control.runOperationIgnoreSelectionChange(() -> {
      for (int i = 0; i < rowsToRemove.size(); i++) {
        int row = rowsToRemove.get(i);
        model.removeRow(row);
      }
    });
  }
}
