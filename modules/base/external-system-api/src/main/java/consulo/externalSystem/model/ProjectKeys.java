/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.model;

import consulo.externalSystem.model.project.*;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.service.project.ProjectData;

import jakarta.annotation.Nonnull;

/**
 * Holds common project entity {@link Key keys}.
 * 
 * @author Denis Zhdanov
 * @since 4/12/13 11:52 AM
 */
public class ProjectKeys {

  @Nonnull
  public static final Key<ProjectData>           PROJECT            = Key.create(ProjectData.class, 50);
  @Nonnull
  public static final Key<ModuleData>            MODULE             = Key.create(ModuleData.class, 70);
  @Nonnull
  public static final Key<LibraryData>           LIBRARY            = Key.create(LibraryData.class, 90);
  @Nonnull
  public static final Key<ContentRootData>       CONTENT_ROOT       = Key.create(ContentRootData.class, 110);
  @Nonnull
  public static final Key<ModuleDependencyData>  MODULE_DEPENDENCY  = Key.create(ModuleDependencyData.class, 130);
  @Nonnull
  public static final Key<LibraryDependencyData> LIBRARY_DEPENDENCY = Key.create(LibraryDependencyData.class, 150);

  @Nonnull
  public static final Key<TaskData> TASK = Key.create(TaskData.class, 250);

  private ProjectKeys() {
  }
}
