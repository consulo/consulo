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
import consulo.application.ApplicationBundle;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 9/28/12 12:16 PM
 */
public class MoveArrangementMatchingRuleUpAction extends AbstractMoveArrangementRuleAction {

  public MoveArrangementMatchingRuleUpAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.move.up.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.move.up.description"));
  }

  @Override
  protected void fillMappings(@Nonnull ArrangementMatchingRulesControl control, @Nonnull List<int[]> mappings) {
    IntList rows = control.getSelectedModelRows();
    IntLists.reverse(rows);
    int top = -1;
    for (int i = 0; i < rows.size(); i++) {
      int row = rows.get(i);
      if (row == top + 1) {
        mappings.add(new int[] { row, row });
        top++;
      }
      else {
        mappings.add(new int[]{ row, row - 1 });
      }
    } 
  }
}
