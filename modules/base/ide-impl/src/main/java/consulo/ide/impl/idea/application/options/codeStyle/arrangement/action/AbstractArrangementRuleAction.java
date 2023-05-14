/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.language.codeStyle.ui.internal.arrangement.ArrangementSectionRulesControl;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Svetlana.Zemlyanskaya
 */
public abstract class AbstractArrangementRuleAction extends AnAction {

  @Nullable
  protected ArrangementMatchingRulesControl getRulesControl(AnActionEvent e) {
    return e.getData(ArrangementSectionRulesControl.KEY);
  }

  protected void scrollRowToVisible(@Nonnull ArrangementMatchingRulesControl control, int row) {
    control.scrollRowToVisible(row);
  }
}
