/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.impl.actions.AboutManager;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ActionImpl(id = "About")
public class AboutAction extends AnAction implements DumbAware {
  private final Provider<AboutManager> myAboutManager;
  private final Provider<WindowManager> myWindowManager;
  private final Application myApplication;

  @Inject
  public AboutAction(Provider<AboutManager> aboutManager, Provider<WindowManager> windowManager, Application application) {
    myAboutManager = aboutManager;
    myWindowManager = windowManager;
    myApplication = application;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setVisible(!TopApplicationMenuUtil.isMacSystemMenu);
    e.getPresentation().setDescription("Show information about " + myApplication.getName());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Window window = myWindowManager.get().suggestParentWindow(e.getData(Project.KEY));

    myAboutManager.get().showAsync(window);
  }
}
