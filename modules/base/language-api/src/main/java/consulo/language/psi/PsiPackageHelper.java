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

package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class PsiPackageHelper {
  public static PsiPackageHelper getInstance(Project project) {
    return project.getInstance( PsiPackageHelper.class);
  }

  @Nonnull
  public abstract String getQualifiedName(@Nonnull PsiDirectory directory, final boolean presentable);

  @Nullable
  public abstract PsiDirectoryContainer getDirectoryContainer(@Nonnull PsiDirectory directory);

  public abstract boolean isPackage(PsiDirectory directory);
}
