/*
 * Copyright 2013-2016 consulo.io
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
package consulo.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiCheckedRenameElement;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayFactory;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 7:50/20.05.13
 */
public interface PsiPackage extends PsiDirectoryContainer, PsiQualifiedNamedElement, PsiCheckedRenameElement, NavigationItem {
  public static final PsiPackage[] EMPTY_ARRAY = new PsiPackage[0];

  public static ArrayFactory<PsiPackage> ARRAY_FACTORY = new ArrayFactory<PsiPackage>() {
    @Nonnull
    @Override
    public PsiPackage[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new PsiPackage[count];
    }
  };

  @Nullable
  PsiPackage getParentPackage();

  /**
   * Returns the list of subpackages of this package under all source roots of the project.
   *
   * @return the array of subpackages.
   */
  @Nonnull
  PsiPackage[] getSubPackages();

  /**
   * Returns the list of subpackages of this package in the specified search scope.
   *
   * @param scope the scope in which packages are searched.
   * @return the array of subpackages.
   */
  @Nonnull
  PsiPackage[] getSubPackages(@Nonnull GlobalSearchScope scope);

  /**
   * @return null means default package
   */
  @RequiredReadAction
  @Override
  @Nullable
  @NonNls
  String getName();

  /**
   * This method must be invoked on the package after all directories corresponding
   * to it have been renamed/moved accordingly to qualified name change.
   *
   * @param newQualifiedName the new qualified name of the package.
   */
  void handleQualifiedNameChange(@Nonnull String newQualifiedName);
}
