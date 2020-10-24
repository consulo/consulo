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
package com.intellij.openapi.wm;

import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.ui.BalloonLayout;
import consulo.ui.Window;
import consulo.ui.Rectangle2D;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;

public interface IdeFrame {
  interface Child extends IdeFrame {
    IdeFrame getParentFrame();
  }

  Key<IdeFrame> KEY = Key.create("IdeFrame");

  @Nullable
  StatusBar getStatusBar();

  @Nullable
  Rectangle2D suggestChildFrameBounds();

  @Nullable
  Project getProject();

  void setFrameTitle(String title);

  void setFileTitle(String fileTitle, File ioFile);

  IdeRootPaneNorthExtension getNorthExtension(String key);

  default JComponent getComponent() {
    throw new AbstractMethodError(getClass().getName() + " is not implemented");
  }

  @Nonnull
  Window getWindow();

  @Nullable
  BalloonLayout getBalloonLayout();

  default boolean isActive() {
    return Window.getActiveWindow() == getWindow();
  }

  default boolean isInFullScreen() {
    return false;
  }
}
