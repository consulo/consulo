/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.impl.roots.ui.configuration.impl.DefaultLibrariesConfigurator;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class StructureLibraryTableModifiableModelProvider implements LibraryTableModifiableModelProvider {
  private final String myLevel;
  private final DefaultLibrariesConfigurator myConfigurator;

  public StructureLibraryTableModifiableModelProvider(String level, DefaultLibrariesConfigurator configurator) {
    myLevel = level;
    myConfigurator = configurator;
  }

  @Nonnull
  @Override
  public LibrariesModifiableModel getModifiableModel() {
    return myConfigurator.getLibrariesModifiableModel(myLevel);
  }
}
