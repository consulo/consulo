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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.ide.IdeBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.impl.idea.openapi.fileChooser.ex.PathField;
import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;

public class TogglePathShowingAction extends AnAction implements DumbAware {
  public TogglePathShowingAction() {
    setEnabledInModalContext(true);
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent e) {
    e.getPresentation().setText(IdeBundle.message("file.chooser.hide.path.tooltip.text"));
    e.getPresentation().setEnabled(e.getDataContext().getData(PathField.PATH_FIELD) != null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(final AnActionEvent e) {
    PathField f = e.getDataContext().getData(PathField.PATH_FIELD);
    if (f != null) {
      f.toggleVisible();
    }
  }
}
