/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ActionPromoter;
import consulo.ui.ex.action.AnAction;
import consulo.dataContext.DataContext;
import consulo.versionControlSystem.ui.Refreshable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class VcsActionPromoter implements ActionPromoter {
  @Override
  public List<AnAction> promote(List<AnAction> actions, DataContext context) {
    if (context.hasData(Refreshable.PANEL_KEY)) {
      for (AnAction action : actions) {
        if (action instanceof ShowMessageHistoryAction) {
          return Arrays.asList(action);
        }
      }
    }
    return Collections.emptyList();
  }
}
