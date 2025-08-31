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

package consulo.language.editor.refactoring.rename;

import consulo.language.editor.TargetElementUtil;
import consulo.language.psi.PsiElement;
import consulo.usage.NonCodeUsageInfo;
import consulo.usage.UsageInfoFactory;
import consulo.usage.UsageInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class NonCodeUsageInfoFactory implements UsageInfoFactory {
  private final PsiElement myElement;
  private final String myStringToReplace;

  public NonCodeUsageInfoFactory(PsiElement element, String stringToReplace) {
    myElement = element;
    myStringToReplace = stringToReplace;
  }

  @Override
  @Nullable
  public UsageInfo createUsageInfo(@Nonnull PsiElement usage, int startOffset, int endOffset) {
    PsiElement namedElement = TargetElementUtil.getNamedElement(usage, startOffset);
    if (namedElement != null) {
      return null;
    }

    int start = usage.getTextRange().getStartOffset();
    return NonCodeUsageInfo.create(usage.getContainingFile(), start + startOffset, start + endOffset, myElement, myStringToReplace);
  }
}