/*
 * Copyright 2013-2023 consulo.io
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
package consulo.project.content.library;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.content.library.LibraryTable;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09/01/2023
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ProjectLibraryTable extends LibraryTable {
  public static final String PROJECT_LEVEL = "project";

  @Nonnull
  @Deprecated
  @DeprecationInfo("Better use constructor injecting")
  static ProjectLibraryTable getInstance(@Nonnull Project project) {
    return project.getInstance(ProjectLibraryTable.class);
  }

  @Nonnull
  Project getProject();

  @Override
  default String getTableLevel() {
    return PROJECT_LEVEL;
  }
}
