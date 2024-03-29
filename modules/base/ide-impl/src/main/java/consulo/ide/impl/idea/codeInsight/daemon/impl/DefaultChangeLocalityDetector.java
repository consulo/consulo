/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ChangeLocalityDetector;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;

@ExtensionImpl(order = "last")
public class DefaultChangeLocalityDetector implements ChangeLocalityDetector {
  @Override
  public PsiElement getChangeHighlightingDirtyScopeFor(@Nonnull PsiElement changedElement) {
    if (changedElement instanceof PsiWhiteSpace ||
        changedElement instanceof PsiComment
        && !changedElement.getText().contains(SuppressionUtil.SUPPRESS_INSPECTIONS_TAG_NAME)) {
      return changedElement;
    }
    return null;
  }
}
