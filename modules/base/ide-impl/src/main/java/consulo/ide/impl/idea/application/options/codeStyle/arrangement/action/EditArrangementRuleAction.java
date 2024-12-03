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

import consulo.application.dumb.DumbAware;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementMatchingRulesControl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Toggleable;
import consulo.util.collection.primitive.ints.IntList;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 10/29/12 11:01 AM
 */
public class EditArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware, Toggleable {
    public EditArrangementRuleAction() {
        super(ApplicationLocalize.arrangementActionRuleEditText(), ApplicationLocalize.arrangementActionRuleEditDescription(), PlatformIconGroup.actionsEdit());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        ArrangementMatchingRulesControl control = getRulesControl(e);
        e.getPresentation().setEnabled(control != null && control.getSelectedModelRows().size() == 1);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
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
