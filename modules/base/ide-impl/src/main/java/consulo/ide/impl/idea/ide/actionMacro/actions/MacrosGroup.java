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
package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.ide.impl.idea.ide.actionMacro.ActionMacro;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.internal.ActionManagerEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

/**
 * @author max
 * @since 2003-07-22
 */
public class MacrosGroup extends ActionGroup {
  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    ArrayList<AnAction> actions = new ArrayList<AnAction>();
    final ActionManagerEx actionManager = ((ActionManagerEx) ActionManager.getInstance());
    String[] ids = actionManager.getActionIds(ActionMacro.MACRO_ACTION_PREFIX);

    for (String id : ids) {
      actions.add(actionManager.getAction(id));
    }

    return actions.toArray(new AnAction[actions.size()]);
  }


}
