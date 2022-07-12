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
package consulo.ide.impl.ui.docking.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.project.ui.wm.dock.DragSession;
import consulo.ide.impl.ui.docking.BaseDockManager;
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
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
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
