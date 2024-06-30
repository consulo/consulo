
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;

public class DeleteAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DeleteAction.class);

  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    DeleteProvider provider = getDeleteProvider(dataContext);
    if (provider == null) return;
    try {
      provider.deleteElement(dataContext);
    }
    catch (Throwable t) {
      if (t instanceof StackOverflowError){
        t.printStackTrace();
      }
      LOG.error(t);
    }
  }

  @Nullable
  protected DeleteProvider getDeleteProvider(DataContext dataContext) {
    return dataContext.getData(PlatformDataKeys.DELETE_ELEMENT_PROVIDER);
  }

  public void update(AnActionEvent event){
    String place = event.getPlace();
    Presentation presentation = event.getPresentation();
    if (ActionPlaces.PROJECT_VIEW_POPUP.equals(place) || ActionPlaces.COMMANDER_POPUP.equals(place)) {
      presentation.setTextValue(IdeLocalize.actionDeleteEllipsis());
    }
    else {
      presentation.setTextValue(IdeLocalize.actionDelete());
    }

    DataContext dataContext = event.getDataContext();
    Project project = event.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    DeleteProvider provider = getDeleteProvider(dataContext);
    if (event.getInputEvent() instanceof KeyEvent keyEvent) {
      Object component = event.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
      if (component instanceof JTextComponent) provider = null; // Do not override text deletion
      if (keyEvent.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
        // Do not override text deletion in speed search
        if (component instanceof JComponent) {
          SpeedSearchSupply searchSupply = SpeedSearchSupply.getSupply((JComponent)component);
          if (searchSupply != null) provider = null;
        }

        String activeSpeedSearchFilter = event.getData(SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY);
        if (!StringUtil.isEmpty(activeSpeedSearchFilter)) {
          provider = null;
        }
      }
    }
    if (provider instanceof TitledHandler titledHandler) {
      presentation.setText(titledHandler.getActionTitle());
    }
    final boolean canDelete = provider != null && provider.canDeleteElement(dataContext);
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setVisible(canDelete);
    }
    else {
      presentation.setEnabled(canDelete);
    }
  }

  public DeleteAction(String text, String description, Image icon) {
    super(text, description, icon);
  }

  public DeleteAction() {
  }
}
