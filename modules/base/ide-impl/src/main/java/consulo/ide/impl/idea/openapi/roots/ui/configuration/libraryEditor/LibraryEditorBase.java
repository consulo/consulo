/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.content.OrderRootType;
import consulo.content.library.LibraryProperties;
import consulo.content.library.LibraryType;
import consulo.content.library.OrderRoot;
import consulo.content.library.ui.LibraryEditor;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public abstract class LibraryEditorBase implements LibraryEditor {
  @Override
  public void removeAllRoots() {
    List<OrderRootType> types = new ArrayList<OrderRootType>(getOrderRootTypes());
    for (OrderRootType type : types) {
      String[] urls = getUrls(type);
      for (String url : urls) {
        removeRoot(url, type);
      }
    }
  }

  protected abstract Collection<OrderRootType> getOrderRootTypes();

  public abstract void setProperties(LibraryProperties properties);

  public abstract void setType(@Nonnull LibraryType<?> type);

  @Override
  public void addRoots(Collection<? extends OrderRoot> roots) {
    for (OrderRoot root : roots) {
      if (root.isJarDirectory()) {
        addJarDirectory(root.getFile(), false, root.getType());
      }
      else {
        addRoot(root.getFile(), root.getType());
      }
    }
  }
}
