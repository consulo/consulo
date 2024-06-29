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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ide.impl.idea.ide.actions.DeleteAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.Presentation;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileChooserKeys;

public class FileDeleteAction extends DeleteAction {
  public FileDeleteAction() {
    setEnabledInModalContext(true);
  }

  protected DeleteProvider getDeleteProvider(DataContext dataContext) {
    return new VirtualFileDeleteProvider();
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    final Boolean available = event.getData(FileChooserKeys.DELETE_ACTION_AVAILABLE);
    if (available != null && !available) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    super.update(event);
  }
}
