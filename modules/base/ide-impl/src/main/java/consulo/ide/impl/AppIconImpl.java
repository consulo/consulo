/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.ui.TaskBar;
import consulo.ui.Window;
import consulo.ui.ex.AppIcon;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 04-Apr-22
 */
@Singleton
@ServiceImpl
public class AppIconImpl implements AppIcon {
  protected Object myCurrentProcessId;

  @Inject
  public AppIconImpl(Application application) {
    ApplicationActivationListener listener = new ApplicationActivationListener() {
      @Override
      public void applicationActivated(IdeFrame ideFrame) {
        setOkBadge(ideFrame.getProject(), false);
      }
    };
    application.getMessageBus().connect().subscribe(ApplicationActivationListener.class, listener);
  }

  @Override
  public boolean setProgress(ComponentManager project, Object processId, TaskBar.ProgressScheme progressScheme, double value, boolean isOk) {
    Window window = getWindow(project);
    if (window != null) {
      return TaskBar.get().setProgress(window, processId, progressScheme, value, isOk);
    }
    return false;
  }

  @Override
  public boolean hideProgress(ComponentManager project, Object processId) {
    if (Objects.equals(myCurrentProcessId, processId)) {
      myCurrentProcessId = null;
    }

    Window window = getWindow(project);

    if (window != null) {
      return TaskBar.get().hideProgress(window, processId);
    }
    else {
      return false;
    }
  }

  @Override
  public void setErrorBadge(ComponentManager project, String text) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().setOkBadge(ideFrame.getWindow(), false);
      TaskBar.get().setTextBadge(ideFrame.getWindow(), text);
    }
  }

  @Override
  public void setOkBadge(ComponentManager project, boolean visible) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().setTextBadge(ideFrame.getWindow(), null);
      TaskBar.get().setOkBadge(ideFrame.getWindow(), visible);
    }
  }

  @Override
  public void requestAttention(ComponentManager project, boolean critical) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().requestAttention(ideFrame.getWindow(), critical);
    }
  }

  @Override
  public void requestFocus(Window frame) {
    if (frame.isActive()) {
      return;
    }

    TaskBar.get().requestFocus(frame);
  }

  @Nullable
  private IdeFrame getIdeFrame(ComponentManager project) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame((Project)project);
    if (frame != null) {
      return frame;
    }
    return null;
  }

  @Nullable
  private Window getWindow(ComponentManager project) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame((Project)project);
    if (frame != null) {
      return frame.getWindow();
    }
    return null;
  }
}
