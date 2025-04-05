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

import consulo.application.Application;
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;

import java.awt.*;

public class EditSourceForDialogAction extends EditSourceAction {
  @Nonnull
  private final Component mySourceComponent;

  public EditSourceForDialogAction(@Nonnull Component component) {
    super();
    Presentation presentation = getTemplatePresentation();
    presentation.setTextValue(ActionLocalize.actionEditsourceText());
    presentation.setIcon(PlatformIconGroup.actionsEditsource());
    presentation.setDescriptionValue(ActionLocalize.actionEditsourceDescription());
    mySourceComponent = component;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Navigatable[] navigatableArray = e.getData(Navigatable.KEY_OF_ARRAY);
    if (navigatableArray != null && navigatableArray.length > 0) {
      Application.get().invokeLater(() -> OpenSourceUtil.navigate(navigatableArray));
      DialogWrapper dialog = DialogWrapper.findInstance(mySourceComponent);
      if (dialog != null && dialog.isModal()) {
        dialog.doCancelAction();
      }
    }
  }
}
