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
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.ui.ex.action.ActionsBundle;
import consulo.application.ApplicationManager;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.navigation.Navigatable;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;

import java.awt.*;

public class EditSourceForDialogAction extends EditSourceAction {
  @Nonnull
  private final Component mySourceComponent;

  public EditSourceForDialogAction(@Nonnull Component component) {
    super();
    Presentation presentation = getTemplatePresentation();
    presentation.setText(ActionsBundle.actionText("EditSource"));
    presentation.setIcon(AllIcons.Actions.EditSource);
    presentation.setDescription(ActionsBundle.actionDescription("EditSource"));
    mySourceComponent = component;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Navigatable[] navigatableArray = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
    if (navigatableArray != null && navigatableArray.length > 0) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          OpenSourceUtil.navigate(navigatableArray);
        }
      });
      DialogWrapper dialog = DialogWrapper.findInstance(mySourceComponent);
      if (dialog != null && dialog.isModal()) {
        dialog.doCancelAction();
      }
    }
  }
}
