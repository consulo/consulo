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
package consulo.ide.impl.idea.diagnostic;

import consulo.compiler.artifact.ArtifactManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * @author stathik
 * @since 2003-11-06
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class DropAnErrorAction extends DumbAwareAction {
  public DropAnErrorAction() {
    super("Drop an error");
  }

  public void actionPerformed(AnActionEvent e) {
    /*
    Project p = (Project)e.getDataContext().getData(DataConstants.PROJECT);
    final StatusBar bar = WindowManager.getInstance().getStatusBar(p);
    bar.fireNotificationPopup(new JLabel("<html><body><br><b>       Notifier      </b><br><br></body></html>"));
    */

    ArtifactManager.getInstance(e.getData(Project.KEY));
    Logger.getInstance("test").error("Test");
  }
}
