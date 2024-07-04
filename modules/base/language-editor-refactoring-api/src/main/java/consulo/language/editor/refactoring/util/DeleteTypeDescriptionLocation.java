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

package consulo.language.editor.refactoring.util;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.*;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class DeleteTypeDescriptionLocation extends ElementDescriptionLocation {
  private final boolean myPlural;

  private DeleteTypeDescriptionLocation(final boolean plural) {
    myPlural = plural;
  }

  public static final DeleteTypeDescriptionLocation SINGULAR = new DeleteTypeDescriptionLocation(false);
  public static final DeleteTypeDescriptionLocation PLURAL = new DeleteTypeDescriptionLocation(true);

  private static final ElementDescriptionProvider ourDefaultProvider = new DefaultProvider();

  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return ourDefaultProvider;
  }

  public boolean isPlural() {
    return myPlural;
  }

  public static class DefaultProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@Nonnull final PsiElement element, @Nonnull final ElementDescriptionLocation location) {
      if (location instanceof DeleteTypeDescriptionLocation deleteTypeDescriptionLocation) {
        final boolean plural = deleteTypeDescriptionLocation.isPlural();
        final int count = plural ? 2 : 1;
        if (element instanceof PsiFileSystemItem psiFileSystemItem && PsiUtilBase.isSymLink(psiFileSystemItem)) {
          return RefactoringLocalize.promptDeleteSymlink(count).get();
        }
        if (element instanceof PsiFile) {
          return RefactoringLocalize.promptDeleteFile(count).get();
        }
        if (element instanceof PsiDirectory) {
          return RefactoringLocalize.promptDeleteDirectory(count).get();
        }
        if (!plural) {
          return FindUsagesProvider.forLanguage(element.getLanguage()).getType(element);
        }
        return "elements";
      }
      return null;
    }
  }
}
