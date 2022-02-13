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
package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.editor.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * See {@link InspectionManager#createProblemDescriptor(PsiElement, String, LocalQuickFix, ProblemHighlightType, boolean) } for method descriptions.
 */
public interface ProblemDescriptor extends CommonProblemDescriptor {
  ProblemDescriptor[] EMPTY_ARRAY = new ProblemDescriptor[0];

  @Nullable
  @RequiredReadAction
  PsiElement getPsiElement();

  @Nullable
  @RequiredReadAction
  PsiElement getStartElement();

  @Nullable
  @RequiredReadAction
  PsiElement getEndElement();

  @Nullable
  TextRange getTextRangeInElement();

  int getLineNumber();

  @Nonnull
  ProblemHighlightType getHighlightType();

  boolean isAfterEndOfLine();

  /**
   * Sets custom attributes for highlighting the inspection result. Can be used only when the severity of the problem is INFORMATION.
   *
   * @param key the text attributes key for highlighting the result.
   * @since 9.0
   */
  void setTextAttributes(TextAttributesKey key);

  /**
   * Gets the unique object, which is the same for all of the problems of this group
   *
   * @return the problem group
   */
  @Nullable
  ProblemGroup getProblemGroup();

  /**
   * Sets the unique object, which is the same for all of the problems of this group
   *
   * @param problemGroup the problemGroup
   */
  void setProblemGroup(@Nullable ProblemGroup problemGroup);

  boolean showTooltip();
}
