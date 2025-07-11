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
package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.RunnerContentUi;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ToggleToolbarLayoutAction extends ToggleAction {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (getRunnerUi(e) == null) {
      e.getPresentation().setEnabled(false);
    }
    else {
      super.update(e);
    }
  }

  @Override
  public boolean isSelected(final AnActionEvent e) {
    final RunnerContentUi ui = getRunnerUi(e);
    return ui != null && ui.isHorizontalToolbar();
  }

  @Override
  public void setSelected(final AnActionEvent e, final boolean state) {
    getRunnerUi(e).setHorizontalToolbar(state);
  }

  @Nullable
  public static RunnerContentUi getRunnerUi(final AnActionEvent e) {
    return e.getDataContext().getData(RunnerContentUi.KEY);
  }

}
