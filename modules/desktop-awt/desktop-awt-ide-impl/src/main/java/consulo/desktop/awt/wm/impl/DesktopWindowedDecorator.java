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
package consulo.desktop.awt.wm.impl;

import consulo.project.Project;
import consulo.ui.ex.awt.FrameWrapper;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.ex.toolWindow.ToolWindowWindowedDecorator;
import jakarta.annotation.Nonnull;

public final class DesktopWindowedDecorator extends FrameWrapper implements ToolWindowWindowedDecorator {
  private final Project myProject;

  DesktopWindowedDecorator(@Nonnull Project project, @Nonnull WindowInfoImpl info, @Nonnull DesktopInternalDecorator internalDecorator) {
    super(project);
    myProject = project;
    setTitle(info.getId() + " - " + myProject.getName());
    setProject(project);
    setComponent(internalDecorator);
  }

  public Project getProject() {
    return myProject;
  }
}