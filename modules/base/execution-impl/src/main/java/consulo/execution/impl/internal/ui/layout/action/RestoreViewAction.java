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

import consulo.execution.impl.internal.ui.layout.CellTransform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.content.Content;

public class RestoreViewAction extends AnAction {

  private final Content myContent;
  private final CellTransform.Restore myRestoreAction;

  public RestoreViewAction(final Content content, CellTransform.Restore restore) {
    myContent = content;
    myRestoreAction = restore;
  }

  @Override
  public void update(final AnActionEvent e) {
    Presentation p = e.getPresentation();
    p.setTextValue(ActionLocalize.actionRunnerRestoreviewText(myContent.getDisplayName()));
    p.setDescriptionValue(ActionLocalize.actionRunnerRestoreviewDescription());
    p.setIcon(myContent.getIcon());
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    myRestoreAction.restoreInGrid();
  }

  public Content getContent() {
    return myContent;
  }
}
