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
package consulo.web.internal.wm;

import com.vaadin.flow.component.UI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.*;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.application.WebApplication;
import consulo.web.internal.servlet.VaadinRootLayout;
import consulo.web.internal.ui.WebRootPaneImpl;
import consulo.web.internal.ui.base.TargetVaddin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
public class WebIdeFrameImpl implements IdeFrameEx, Disposable {
  private final Project myProject;
  private final WebIdeRootView myRootView;

  private UnifiedStatusBarImpl myStatusBar;

  private VaadinRootLayout myRootLayout;

  public WebIdeFrameImpl(Project project) {
    myProject = project;
    myRootView = new WebIdeRootView(project);
  }

  @RequiredUIAccess
  public void show() {
    UI ui = UI.getCurrent();

    VaadinRootLayout view = (VaadinRootLayout)ui.getCurrentView();

    myRootLayout = view;

    String projectTitle = FrameTitleBuilder.getInstance().getProjectTitle(myProject);

    ui.getPage().setTitle(projectTitle);

    myStatusBar = new UnifiedStatusBarImpl(myProject.getApplication(), null);
    Disposer.register(this, myStatusBar);
    myStatusBar.install(this);

    myRootView.setStatusBar(myStatusBar);

    StatusBarWidgetsManager.getInstance(myProject).updateAllWidgets(UIAccess.current());

    myRootView.update();
    
    myRootLayout.update(TargetVaddin.to(myRootView.getRootPanel().getComponent()));
  }

  public WebRootPaneImpl getRootPanel() {
    return myRootView.getRootPanel();
  }

  @Nonnull
  @Override
  public Window getWindow() {
    return (Window)Objects.requireNonNull(myRootLayout).toUIComponent();
  }

  public void close() {
    WebApplication.invokeOnCurrentSession(() -> {
      UI.getCurrent().getPage().executeJs("window.close();");
    });
  }

  @Override
  public StatusBar getStatusBar() {
    return myStatusBar;
  }

  @Override
  public Rectangle2D suggestChildFrameBounds() {
    return null;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void setFrameTitle(String title) {

  }

  @Override
  public void setFileTitle(String fileTitle, File ioFile) {

  }

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return null;
  }

  @Nullable
  @Override
  public BalloonLayout getBalloonLayout() {
    return null;
  }

  @Override
  public void dispose() {

  }
}
