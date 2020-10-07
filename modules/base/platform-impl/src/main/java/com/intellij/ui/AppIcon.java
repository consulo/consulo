/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import consulo.ui.TaskBar;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

// TODO [VISTALL] migrate to service!!!
public class AppIcon {
  private static AppIcon ourInstance = new AppIcon();
  protected Object myCurrentProcessId;

  @Nonnull
  public static AppIcon getInstance() {
    return ourInstance;
  }

  private AppIcon() {
    ApplicationActivationListener listener = new ApplicationActivationListener() {
      @Override
      public void applicationActivated(IdeFrame ideFrame) {
        setOkBadge(ideFrame.getProject(), false);
      }
    };
    Application.get().getMessageBus().connect().subscribe(ApplicationActivationListener.TOPIC, listener);
  }

  public boolean setProgress(Project project, Object processId, TaskBar.ProgressScheme progressScheme, double value, boolean isOk) {
    Window window = getWindow(project);
    if (window != null) {
      return TaskBar.get().setProgress(window, processId, progressScheme, value, isOk);
    }
    return false;
  }

  public boolean hideProgress(Project project, Object processId) {
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

  public void setErrorBadge(Project project, String text) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().setOkBadge(ideFrame.getWindow(), false);
      TaskBar.get().setTextBadge(ideFrame.getWindow(), text);
    }
  }

  public void setOkBadge(Project project, boolean visible) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().setTextBadge(ideFrame.getWindow(), null);
      TaskBar.get().setOkBadge(ideFrame.getWindow(), visible);
    }
  }

  public void requestAttention(Project project, boolean critical) {
    IdeFrame ideFrame = getIdeFrame(project);

    if (ideFrame != null && !ideFrame.isActive()) {
      TaskBar.get().requestAttention(ideFrame.getWindow(), critical);
    }
  }

  public void requestFocus(IdeFrame frame) {
    if (frame.isActive()) {
      return;
    }

    TaskBar.get().requestFocus(frame.getWindow());
  }

  @Nullable
  private IdeFrame getIdeFrame(Project project) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
    if (frame != null) {
      return frame;
    }
    return null;
  }

  @Nullable
  private Window getWindow(Project project) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
    if (frame != null) {
      return frame.getWindow();
    }
    return null;
  }
}