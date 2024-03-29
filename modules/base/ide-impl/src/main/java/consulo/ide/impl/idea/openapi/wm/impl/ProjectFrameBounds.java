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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import consulo.component.persist.PersistentStateComponent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.*;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "ProjectFrameBounds", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class ProjectFrameBounds implements PersistentStateComponent<Rectangle> {
  public static ProjectFrameBounds getInstance(Project project) {
    return ServiceManager.getService(project, ProjectFrameBounds.class);
  }

  private final Project myProject;
  private Rectangle myBounds;

  @Inject
  public ProjectFrameBounds(Project project) {
    myProject = project;
  }

  @Override
  public Rectangle getState() {
    return TargetAWT.to(WindowManager.getInstance().getWindow(myProject)).getBounds();
  }

  @Override
  public void loadState(Rectangle state) {
    myBounds = state;
  }

  public Rectangle getBounds() {
    return myBounds;
  }
}
