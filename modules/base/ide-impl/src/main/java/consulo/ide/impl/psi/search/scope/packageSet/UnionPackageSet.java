/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.psi.search.scope.packageSet;

import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.PackageSetBase;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

public class UnionPackageSet extends PackageSetBase {
  private final PackageSet myFirstSet;
  private final PackageSet mySecondSet;

  public UnionPackageSet(@Nonnull PackageSet set1, @Nonnull PackageSet set2) {
    myFirstSet = set1;
    mySecondSet = set2;
  }

  @Override
  public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
    return myFirstSet.contains(file, project, holder) || mySecondSet.contains(file, project, holder);
  }

  @Override
  @Nonnull
  public PackageSet createCopy() {
    return new UnionPackageSet(myFirstSet.createCopy(), mySecondSet.createCopy());
  }

  @Override
  public int getNodePriority() {
    return 3;
  }

  @Override
  @Nonnull
  public String getText() {
    return myFirstSet.getText() + "||" + mySecondSet.getText();
  }

  public PackageSet getFirstSet() {
    return myFirstSet;
  }

  public PackageSet getSecondSet() {
    return mySecondSet;
  }
}