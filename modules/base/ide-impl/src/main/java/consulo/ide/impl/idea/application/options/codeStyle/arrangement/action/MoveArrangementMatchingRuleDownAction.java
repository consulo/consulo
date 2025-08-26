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

import consulo.annotation.component.ActionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementConstants;
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementMatchingRulesControl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.collection.primitive.ints.IntList;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 2012-09-28
 */
@ActionImpl(id = ArrangementConstants.MATCHING_RULE_MOVE_DOWN)
public class MoveArrangementMatchingRuleDownAction extends AbstractMoveArrangementRuleAction {
    public MoveArrangementMatchingRuleDownAction() {
        super(
            ApplicationLocalize.arrangementActionRuleMoveDownText(),
            ApplicationLocalize.arrangementActionRuleMoveDownDescription(),
            PlatformIconGroup.actionsMovedown()
        );
    }

    @Override
    protected void fillMappings(@Nonnull ArrangementMatchingRulesControl control, @Nonnull List<int[]> mappings) {
        IntList rows = control.getSelectedModelRows();
        int bottom = control.getModel().getSize();
        for (int i = 0; i < rows.size(); i++) {
            int row = rows.get(i);
            if (row == bottom - 1) {
                mappings.add(new int[]{row, row});
                bottom--;
            }
            else {
                mappings.add(new int[]{row, row + 1});
            }
        }
    }
}
