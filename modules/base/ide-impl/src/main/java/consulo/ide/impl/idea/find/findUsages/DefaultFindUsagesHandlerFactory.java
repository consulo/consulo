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
package consulo.ide.impl.idea.find.findUsages;

import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;

import jakarta.annotation.Nonnull;

/**
 * @author peter
*/
@ExtensionImpl(id = "default", order = "last")
public final class DefaultFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
  @Override
  public boolean canFindUsages(@Nonnull final PsiElement element) {
    if (element instanceof PsiFileSystemItem) {
      if (((PsiFileSystemItem)element).getVirtualFile() == null) return false;
    }
    else if (!FindUsagesProvider.forLanguage(element.getLanguage()).canFindUsagesFor(element)) {
      return false;
    }
    return element.isValid();
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@Nonnull final PsiElement element, final boolean forHighlightUsages) {
    if (canFindUsages(element)) {
      return new FindUsagesHandler(element){};
    }
    return null;
  }
}
