/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.AllIcons;
import consulo.navigation.Navigatable;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;

import javax.swing.*;

public class EditSourceInCommitAction extends AnAction {
  private final DialogWrapper myDialogWrapper;

  public EditSourceInCommitAction(final DialogWrapper dialogWrapper) {
    super(
      ActionLocalize.actionEditsourceText(),
      ActionLocalize.actionEditsourceDescription(),
      AllIcons.Actions.EditSource
    );
    myDialogWrapper = dialogWrapper;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Navigatable[] navigatableArray = e.getData(Navigatable.KEY_OF_ARRAY);
    if (navigatableArray != null && navigatableArray.length > 0) {
      SwingUtilities.invokeLater(() -> OpenSourceUtil.navigate(navigatableArray));
      myDialogWrapper.doCancelAction();
    }
  }
}
