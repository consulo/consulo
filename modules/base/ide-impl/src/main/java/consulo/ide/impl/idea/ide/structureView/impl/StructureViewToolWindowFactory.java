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

package consulo.ide.impl.idea.ide.structureView.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.structureView.StructureViewFactory;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class StructureViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Nonnull
  @Override
  public String getId() {
    return ToolWindowId.STRUCTURE_VIEW;
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(Project project, ToolWindow toolWindow) {
    StructureViewFactoryImpl factory = (StructureViewFactoryImpl)StructureViewFactory.getInstance(project);
    factory.initToolWindow((ToolWindowEx)toolWindow);
  }

  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.LEFT;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.toolwindowsToolwindowstructure();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Structure");
  }

  @Override
  public boolean isSecondary() {
    return true;
  }
}
