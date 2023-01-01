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

/**
 * @author VISTALL
 * @since 04-Sep-22
 */
public class ModuleLibraryTablePresentation extends LibraryTablePresentation {
  public static final ModuleLibraryTablePresentation INSTANCE = new ModuleLibraryTablePresentation();

  private ModuleLibraryTablePresentation() {
  }

  @Override
  public String getDisplayName(boolean plural) {
    return ProjectBundle.message("module.library.display.name", plural ? 2 : 1);
  }

  @Override
  public String getDescription() {
    return ProjectBundle.message("libraries.node.text.module");
  }

  @Override
  public String getLibraryTableEditorTitle() {
    return ProjectBundle.message("library.configure.module.title");
  }
}
