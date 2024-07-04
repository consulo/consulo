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

package consulo.language.editor.refactoring.safeDelete;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.UsageViewDescriptorAdapter;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewBundle;
import jakarta.annotation.Nonnull;

/**
 * @author dsl
 */
public class SafeDeleteUsageViewDescriptor extends UsageViewDescriptorAdapter {
  private final PsiElement[] myElementsToDelete;

  public SafeDeleteUsageViewDescriptor(PsiElement[] elementsToDelete) {
    myElementsToDelete = elementsToDelete;
  }

  @Override
  @Nonnull
  public PsiElement[] getElements() {
    return myElementsToDelete;
  }

  @Override
  public String getProcessedElementsHeader() {
    return RefactoringLocalize.itemsToBeDeleted().get();
  }

  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringLocalize.referencesInCode(UsageViewBundle.getReferencesString(usagesCount, filesCount)).get();
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return RefactoringLocalize.safeDeleteCommentOccurencesHeader(UsageViewBundle.getOccurencesString(usagesCount, filesCount)).get();
  }
}
