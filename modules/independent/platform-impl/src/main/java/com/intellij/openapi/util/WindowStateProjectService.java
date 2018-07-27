/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.impl.ProjectFrameBounds;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

/**
 * @author Sergey.Malenkov
 */
@State(name = "WindowStateProjectService", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@Singleton
final class WindowStateProjectService extends WindowStateServiceImpl implements WindowStateService.ProjectLevel {
  private final Project myProject;

  @Inject
  WindowStateProjectService(Project project) {
    myProject = project;
  }

  @Override
  Point getDefaultLocationFor(Object object, @Nonnull String key) {
    Rectangle bounds = getDefaultBoundsFor(object, key);
    return bounds == null ? null : bounds.getLocation();
  }

  @Override
  Dimension getDefaultSizeFor(Object object, @Nonnull String key) {
    Rectangle bounds = getDefaultBoundsFor(object, key);
    return bounds == null ? null : bounds.getSize();
  }

  @Override
  Rectangle getDefaultBoundsFor(Object object, @Nonnull String key) {
    //  backward compatibility when this service is used instead of ProjectFrameBounds
    return !key.equals("ProjectFrameBounds") ? null : ProjectFrameBounds.getInstance(myProject).getBounds();
  }

  @Override
  boolean getDefaultMaximizedFor(Object object, @Nonnull String key) {
    return false;
  }
}
