/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.log.VcsLogUi;
import jakarta.annotation.Nonnull;

public class ShowRootsColumnAction extends BooleanPropertyToggleAction {

  public ShowRootsColumnAction() {
    super("Show Root Names");
  }

  @Override
  protected VcsLogUiProperties.VcsLogUiProperty<Boolean> getProperty() {
    return MainVcsLogUiProperties.SHOW_ROOT_NAMES;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    VcsLogUi ui = e.getData(VcsLogUi.KEY);
    if (ui == null || !ui.isMultipleRoots()) e.getPresentation().setEnabledAndVisible(false);
  }
}
