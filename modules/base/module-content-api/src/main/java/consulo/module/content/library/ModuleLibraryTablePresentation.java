/*
 * Copyright 2013-2022 consulo.io
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
package consulo.module.content.library;

import consulo.content.library.LibraryTablePresentation;
import consulo.project.ProjectBundle;
import consulo.project.localize.ProjectLocalize;

/**
 * @author VISTALL
 * @since 2022-09-04
 */
public class ModuleLibraryTablePresentation extends LibraryTablePresentation {
  public static final ModuleLibraryTablePresentation INSTANCE = new ModuleLibraryTablePresentation();

  private ModuleLibraryTablePresentation() {
  }

  @Override
  public String getDisplayName(boolean plural) {
    return ProjectLocalize.moduleLibraryDisplayName(plural ? 2 : 1).get();
  }

  @Override
  public String getDescription() {
    return ProjectLocalize.librariesNodeTextModule().get();
  }

  @Override
  public String getLibraryTableEditorTitle() {
    return ProjectLocalize.libraryConfigureModuleTitle().get();
  }
}
