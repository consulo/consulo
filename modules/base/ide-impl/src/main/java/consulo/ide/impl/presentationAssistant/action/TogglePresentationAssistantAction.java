/*
 * Copyright 2013-2017 consulo.io
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

package consulo.ide.impl.presentationAssistant.action;


import consulo.ide.impl.presentationAssistant.PresentationAssistant;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2017-08-21
 */
public class TogglePresentationAssistantAction extends ToggleAction {
  public TogglePresentationAssistantAction() {
    super("Presentation Mouse/Keyboard Assistant");
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    return PresentationAssistant.getInstance().getConfiguration().myShowActionDescriptions;
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    PresentationAssistant.getInstance().setShowActionsDescriptions(state, e.getData(Project.KEY));
  }
}
