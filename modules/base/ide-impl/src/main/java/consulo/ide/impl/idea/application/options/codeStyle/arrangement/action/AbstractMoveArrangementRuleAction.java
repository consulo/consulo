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
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 2012-11-13
 */
public abstract class AbstractMoveArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware {
    protected AbstractMoveArrangementRuleAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ArrangementMatchingRulesControl control = getRulesControl(e);
        if (control == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        List<int[]> mappings = new ArrayList<>();
        fillMappings(control, mappings);
        for (int[] mapping : mappings) {
            if (mapping[0] != mapping[1]) {
                e.getPresentation().setEnabled(true);
                return;
            }
        }
        e.getPresentation().setEnabled(false);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ArrangementMatchingRulesControl control = getRulesControl(e);
        if (control == null) {
            return;
        }

        int editing = control.getEditingRow() - 1;

        control.runOperationIgnoreSelectionChange(() -> {
            control.hideEditor();
            List<int[]> mappings = new ArrayList<>();
            fillMappings(control, mappings);

            if (mappings.isEmpty()) {
                return;
            }

            int newRowToEdit = editing;
            ArrangementMatchingRulesModel model = control.getModel();
            Object value;
            int from, to;
            for (int[] pair : mappings) {
                from = pair[0];
                to = pair[1];
                if (from != to) {
                    value = model.getElementAt(from);
                    model.removeRow(from);
                    model.insert(to, value);
                    if (newRowToEdit == from) {
                        newRowToEdit = to;
                    }
                }
            }

            ListSelectionModel selectionModel = control.getSelectionModel();
            for (int[] pair : mappings) {
                selectionModel.addSelectionInterval(pair[1], pair[1]);
            }

            int visibleRow = -1;
            if (newRowToEdit >= 0) {
                control.showEditor(newRowToEdit);
                visibleRow = newRowToEdit;
            }
            else if (!mappings.isEmpty()) {
                visibleRow = mappings.get(0)[1];
            }

            if (visibleRow != -1) {
                scrollRowToVisible(control, visibleRow);
            }
        });
        control.repaintRows(0, control.getModel().getSize() - 1, true);
    }

    protected abstract void fillMappings(@Nonnull ArrangementMatchingRulesControl control, @Nonnull List<int[]> mappings);
}
