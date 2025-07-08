/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.application.dumb.DumbAware;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author zajac
 * @since 2011-03-05
 */
public class RestorePreviousSettingsAction extends AnAction implements ShortcutProvider, DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabled(
      project != null && search != null && !project.isDisposed()
        && search.getTextInField().isEmpty()
        && FindManager.getInstance(project).getPreviousFindModel() != null
    );
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    FindModel findModel = e.getRequiredData(EditorSearchSession.SESSION_KEY).getFindModel();
    findModel.copyFrom(FindManager.getInstance(e.getRequiredData(Project.KEY)).getPreviousFindModel());
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
  }
}
