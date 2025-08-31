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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.Project;
import consulo.project.util.WaitForProgressToShow;

import java.awt.*;

public abstract class AbstractCalledLater implements Runnable {
  private final Project myProject;
  private final consulo.ui.ModalityState myState;

  protected AbstractCalledLater(Project project, consulo.ui.ModalityState modalityState) {
    myProject = project;
    myState = modalityState;
  }

  protected AbstractCalledLater(Project project, Component component) {
    myProject = project;
    myState = IdeaModalityState.stateForComponent(component);
  }

  public void callMe() {
    WaitForProgressToShow.runOrInvokeLaterAboveProgress(this, myState, myProject);
  }
}
