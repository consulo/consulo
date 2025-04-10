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
package consulo.ide.impl.idea.openapi.module.impl.scopes;

import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.language.internal.LibraryScopeBase;
import consulo.project.Project;

/**
 * @author nik
 */
public class LibraryScope extends LibraryScopeBase {
  private final Library myLibrary;

  public LibraryScope(Project project, Library library) {
    super(project, library.getFiles(BinariesOrderRootType.getInstance()), library.getFiles(SourcesOrderRootType.getInstance()));
    myLibrary = library;
  }

  @Override
  public String getDisplayName() {
    String name = myLibrary.getName();
    if (name == null) {
      String[] urls = myLibrary.getUrls(BinariesOrderRootType.getInstance());
      if (urls.length > 0) {
        name = PathUtil.getFileName(VfsUtilCore.urlToPath(urls[0]));
      }
      else {
        name = "empty";
      }
    }
    return "Library '" + name + "'";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return myLibrary.equals(((LibraryScope)o).myLibrary);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myLibrary.hashCode();
  }
}
