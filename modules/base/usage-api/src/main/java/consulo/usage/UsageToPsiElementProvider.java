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
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to provide appropriate psiElements from usages in Find Usages popup.
 * For instance, it's used in Find Usages popup to help ShowImplementationsAction show
 * psiElement containing a usage
 *
 * @author Konstantin Bulenkov
 */
@Extension(ComponentScope.APPLICATION)
public abstract class UsageToPsiElementProvider {
  public static final ExtensionPointName<UsageToPsiElementProvider> EP_NAME = ExtensionPointName.create(UsageToPsiElementProvider.class);

  @Nullable
  public abstract PsiElement getAppropriateParentFrom(PsiElement element);

  @Nullable
  public static PsiElement findAppropriateParentFrom(@Nonnull PsiElement element) {
    return EP_NAME.computeSafeIfAny(Application.get(), p -> p.getAppropriateParentFrom(element));
  }
}
