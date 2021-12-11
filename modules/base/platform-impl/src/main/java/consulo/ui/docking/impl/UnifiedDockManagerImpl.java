/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.docking.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockableContent;
import com.intellij.ui.docking.DragSession;
import consulo.ui.docking.BaseDockManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
@Singleton
@State(name = "DockManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class UnifiedDockManagerImpl extends BaseDockManager {
  @Inject
  public UnifiedDockManagerImpl(Project project) {
    super(project);
  }

  @Override
  protected DockWindow createWindowFor(@Nullable String id, DockContainer container) {
    return null;
  }

  @Override
  public DragSession createDragSession(MouseEvent mouseEvent, @Nonnull DockableContent content) {
    return null;
  }

  @Override
  public IdeFrame getIdeFrame(DockContainer container) {
    return null;
  }

  @Override
  public String getDimensionKeyForFocus(@Nonnull String key) {
    return null;
  }
}
