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
package consulo.ide.impl.idea.ui.debugger;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class ShowUiDebuggerAction extends AnAction {

  private UiDebugger myDebugger;

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setTextValue(LocalizeValue.localizeTODO("UI Debugger"));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (myDebugger == null) {
      myDebugger = new UiDebugger() {
        @Override
        public void dispose() {
          super.dispose();
          myDebugger = null;
        }
      };
    } else {
      myDebugger.show();
    }
  }
}