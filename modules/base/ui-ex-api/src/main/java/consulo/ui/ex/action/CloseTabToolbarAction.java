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
package consulo.ui.ex.action;

import consulo.application.CommonBundle;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.application.dumb.DumbAware;

public abstract class CloseTabToolbarAction extends AnAction implements DumbAware {
  public CloseTabToolbarAction() {
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE_ACTIVE_TAB));
    Presentation presentation = getTemplatePresentation();
    presentation.setIcon(AllIcons.Actions.Cancel);
    presentation.setText(CommonBundle.getCloseButtonText());
    presentation.setDescription(null);
  }
}
