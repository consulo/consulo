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
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Toggleable;
import consulo.application.ApplicationBundle;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.util.collection.primitive.ints.IntList;

/**
 * @author Denis Zhdanov
 * @since 10/29/12 11:01 AM
 */
public class EditArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware, Toggleable {

  public EditArrangementRuleAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.edit.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.edit.description"));
    getTemplatePresentation().setIcon(IconUtil.getEditIcon());
  }

  @Override
  public void update(AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    e.getPresentation().setEnabled(control != null && control.getSelectedModelRows().size() == 1);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    if (control == null) {
      return;
    }
    IntList rows = control.getSelectedModelRows();
    if (rows.size() != 1) {
      return;
    }
    final int row = rows.get(0);
    control.showEditor(row);
    scrollRowToVisible(control, row);
  }
}
