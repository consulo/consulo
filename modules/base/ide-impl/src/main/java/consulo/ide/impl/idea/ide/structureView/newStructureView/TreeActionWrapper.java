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

package consulo.ide.impl.idea.ide.structureView.newStructureView;

import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.fileEditor.structureView.tree.TreeAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.application.dumb.DumbAware;

public class TreeActionWrapper extends ToggleAction implements DumbAware {
  private final TreeAction myAction;
  private final TreeActionsOwner myStructureView;


  public TreeActionWrapper(TreeAction action, TreeActionsOwner structureView) {
    myAction = action;
    myStructureView = structureView;
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    Presentation presentation = e.getPresentation();
    ActionPresentation actionPresentation = myAction.getPresentation();
    if (presentation.getClientProperty(MenuItemPresentationFactory.HIDE_ICON) == null) {
      presentation.setIcon(actionPresentation.getIcon());
    }
    presentation.setText(actionPresentation.getText());
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return TreeModelWrapper.isActive(myAction, myStructureView);
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    myStructureView.setActionActive(myAction.getName(), TreeModelWrapper.shouldRevert(myAction) ?  !state : state);
  }
}
