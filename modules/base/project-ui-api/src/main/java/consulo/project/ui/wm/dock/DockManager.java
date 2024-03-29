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
package consulo.project.ui.wm.dock;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class DockManager {
  public static DockManager getInstance(Project project) {
    return project.getInstance(DockManager.class);
  }

  public abstract void register(DockContainer container);

  public abstract void register(String id, DockContainerFactory factory);

  public abstract DragSession createDragSession(MouseEvent mouseEvent, @Nonnull DockableContent content);

  public abstract Set<DockContainer> getContainers();

  public abstract IdeFrame getIdeFrame(DockContainer container);

  public abstract String getDimensionKeyForFocus(@Nonnull String key);

  @Nullable
  public DockContainer getContainerFor(Component c) {
    throw new UnsupportedOperationException("desktop only");
  }

  @Nullable
  public abstract DockContainer getContainerFor(consulo.ui.Component c);
}
