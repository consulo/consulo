/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.project.ui.view.tree;

import consulo.module.Module;
import consulo.application.util.Queryable;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiPackage;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 */
public final class PackageElement implements Queryable, RootsProvider {
  public static final Key<PackageElement> DATA_KEY = Key.create("package.element");

  @Nullable
  private final Module myModule;
  @Nonnull
  private final PsiPackage myElement;
  private final boolean myIsLibraryElement;

  public PackageElement(@Nullable Module module, @Nonnull PsiPackage element, boolean isLibraryElement) {
    myModule = module;
    myElement = element;
    myIsLibraryElement = isLibraryElement;
  }

  @Nullable
  public Module getModule() {
    return myModule;
  }

  @Nonnull
  public PsiPackage getPackage() {
    return myElement;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getRoots() {
    if (myModule == null) {
      return List.of();
    }

    Set<VirtualFile> roots = new HashSet<>();
    PsiDirectory[] dirs = PackageNodeUtil.getDirectories(getPackage(), myModule.getProject(), myModule, isLibraryElement());
    for (PsiDirectory each : dirs) {
      roots.add(each.getVirtualFile());
    }
    return roots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PackageElement)) return false;

    PackageElement packageElement = (PackageElement)o;

    if (myIsLibraryElement != packageElement.myIsLibraryElement) return false;
    if (!myElement.equals(packageElement.myElement)) return false;
    if (myModule != null ? !myModule.equals(packageElement.myModule) : packageElement.myModule != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myModule != null ? myModule.hashCode() : 0;
    result = 29 * result + myElement.hashCode();
    result = 29 * result + (myIsLibraryElement ? 1 : 0);
    return result;
  }

  public boolean isLibraryElement() {
    return myIsLibraryElement;
  }


  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    PsiPackage pkg = getPackage();
    if (pkg instanceof Queryable) {
      ((Queryable)pkg).putInfo(info);
    }
  }
}
