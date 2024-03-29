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

package consulo.language.editor.refactoring.safeDelete.usageInfo;

import consulo.logging.Logger;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;

/**
 * @author dsl
 */
public class SafeDeleteReferenceSimpleDeleteUsageInfo extends SafeDeleteReferenceUsageInfo {
  private static final Logger LOG = Logger.getInstance(SafeDeleteReferenceSimpleDeleteUsageInfo.class);
  public SafeDeleteReferenceSimpleDeleteUsageInfo(PsiElement element, PsiElement referencedElement, boolean isSafeDelete) {
    super(element, referencedElement, isSafeDelete);
  }

  public SafeDeleteReferenceSimpleDeleteUsageInfo(PsiElement element, PsiElement referencedElement,
                                                  int startOffset, int endOffset, boolean isNonCodeUsage, boolean isSafeDelete) {
    super(element, referencedElement, startOffset, endOffset, isNonCodeUsage, isSafeDelete);
  }

  @Override
  public void deleteElement() throws IncorrectOperationException {
    if(isSafeDelete()) {
      PsiElement element = getElement();
      LOG.assertTrue(element != null);
      element.delete();
    }
  }
}
