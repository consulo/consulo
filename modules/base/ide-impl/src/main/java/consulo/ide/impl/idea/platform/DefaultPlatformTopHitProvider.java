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
package consulo.ide.impl.idea.platform;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.action.ActionsTopHitProvider;
import consulo.ui.ex.action.IdeActions;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class DefaultPlatformTopHitProvider extends ActionsTopHitProvider {
  private static final String[][] ACTION_MATRIX = {
          {"op", "open ", "OpenFile"},
          {"reo", "reopen ", "$LRU"},
          {"new", "new ", IdeActions.GROUP_NEW},
          {"new c", "new class ", "NewClass"},
          {"new i", "new interface ", "NewClass"},
          {"new e", "new enum ", "NewClass"},
          {"line", "line numbers ", "EditorToggleShowLineNumbers"},
          {"show li", "show line numbers ", "EditorToggleShowLineNumbers"},
          {"ann", "annotate ", "Annotate"},
          {"wrap", "wraps ", IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS},
          {"soft w", "soft wraps ", IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS},
          {"use sof", "use soft wraps ", IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS},
          {"use wr", "use wraps ", IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS},
          {"ref", "refactor ", "Refactorings.QuickListPopupAction"},
          {"mov", "move ", IdeActions.ACTION_MOVE},
          {"ren", "rename  ", IdeActions.ACTION_RENAME},
  };

  @Override
  protected String[][] getActionsMatrix() {
    return ACTION_MATRIX;
  }
}
