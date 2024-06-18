/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.ActionManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author Bas Leijdekkers
 */
@TopicImpl(ComponentScope.APPLICATION)
public class WindowDressing implements ProjectManagerListener {
  @Nonnull
  private final ActionManager myActionManager;

  @Inject
  public WindowDressing(@Nonnull ActionManager actionManager) {
    myActionManager = actionManager;
  }

  public static ProjectWindowActionGroup getWindowActionGroup(ActionManager actionManager1) {
    return (ProjectWindowActionGroup)actionManager1.getAction("OpenProjectWindows");
  }

  @Override
  public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    getWindowActionGroup(myActionManager).addProject(project);
  }

  @Override
  public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    getWindowActionGroup(myActionManager).removeProject(project);
  }
}
