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

/**
 * created at Sep 11, 2001
 * @author Jeka
 */
package consulo.language.editor.refactoring.move;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;

public class MoveMemberViewDescriptor implements UsageViewDescriptor {
  private final PsiElement[] myElementsToMove;

  public MoveMemberViewDescriptor(PsiElement[] elementsToMove) {
    myElementsToMove = elementsToMove;
  }

  @Override
  @Nonnull
  public PsiElement[] getElements() {
    return myElementsToMove;
  }

  @Override
  public String getProcessedElementsHeader() {
    return RefactoringLocalize.moveMembersElementsHeader().get();
  }

  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringLocalize.referencesToBeChanged(UsageViewBundle.getReferencesString(usagesCount, filesCount)).get();
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return null;
  }
}
