/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.navigationToolbar;

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author gregsh
 */
public abstract class AbstractNavBarModelExtension implements NavBarModelExtension {
  @Nullable
  @Override
  public abstract String getPresentableText(Object object);

  @Nullable
  @Override
  public PsiElement adjustElement(PsiElement psiElement) {
    return psiElement;
  }

  @Nullable
  @Override
  public PsiElement getParent(@Nonnull PsiElement psiElement) {
    return null;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> additionalRoots(Project project) {
    return List.of();
  }
}
