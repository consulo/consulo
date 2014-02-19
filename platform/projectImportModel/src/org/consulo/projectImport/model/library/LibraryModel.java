/*
 * Copyright 2013 must-be.org
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
package org.consulo.projectImport.model.library;

import org.consulo.projectImport.model.NamedModelContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 17:34/19.06.13
 */
public class LibraryModel extends NamedModelContainer {
  public LibraryModel(@Nullable String name) {
    super(name);
  }

  @NotNull
  private LibraryRootTableModel getOrCreateTable(@NotNull OrderRootTypeModel typeModel) {
    LibraryRootTableModel child = findChild(LibraryRootTableModel.class);
    if(child == null) {
      addChild(child = new LibraryRootTableModel(typeModel));
    }
    return child;
  }

  public void addUrl(@NotNull OrderRootTypeModel typeModel, @NotNull String url) {
    LibraryRootTableModel child = getOrCreateTable(typeModel);
    child.addUrl(url);
  }

  @NotNull
  public LibraryRootTableModel getRootTable(@NotNull OrderRootTypeModel typeModel) {
    for (LibraryRootTableModel o : findChildren(LibraryRootTableModel.class)) {
      if(o.getRootType() == typeModel) {
        return o;
      }
    }
    return getOrCreateTable(typeModel);
  }
}
