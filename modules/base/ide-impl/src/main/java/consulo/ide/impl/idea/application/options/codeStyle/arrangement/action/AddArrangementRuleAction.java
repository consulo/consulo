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
import consulo.language.codeStyle.ui.internal.arrangement.EmptyArrangementRuleComponent;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationBundle;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.util.collection.primitive.ints.IntList;

import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 2012-08-24
 */
public class AddArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware {
    public AddArrangementRuleAction() {
        getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.add.text"));
        getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.add.description"));
        getTemplatePresentation().setIcon(IconUtil.getAddIcon());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ArrangementMatchingRulesControl control = getRulesControl(e);
        if (control == null) {
            return;
        }

        control.hideEditor();
        IntList rows = control.getSelectedModelRows();
        ArrangementMatchingRulesModel model = control.getModel();
        int rowToEdit;
        if (rows.size() == 1) {
            rowToEdit = rows.get(0) + 1;
            model.insertRow(rowToEdit, new Object[]{createNewRule(control)});
        }
        else {
            rowToEdit = model.getSize();
            model.add(createNewRule(control));
        }
        showEditor(control, rowToEdit);
        control.getSelectionModel().setSelectionInterval(rowToEdit, rowToEdit);
        scrollRowToVisible(control, rowToEdit);
    }

    @Nonnull
    protected Object createNewRule(@Nonnull ArrangementMatchingRulesControl control) {
        return new EmptyArrangementRuleComponent(control.getEmptyRowHeight());
    }

    protected void showEditor(@Nonnull ArrangementMatchingRulesControl control, int rowToEdit) {
        control.showEditor(rowToEdit);
    }
}
