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
package consulo.language.editor.surroundWith;

import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines a single template which can be used in Surround With.
 *
 * @author ven
 * @see SurroundDescriptor
 */
public interface Surrounder {
  /**
   * Returns the user-visible name of the Surround With template.
   *
   * @return the template name
   */
  @Nonnull
  LocalizeValue getTemplateDescription();

  /**
   * Checks if the template can be used to surround the specified range of elements.
   *
   * @param elements the elements to be surrounded
   * @return true if the template is applicable to the elements, false otherwise.
   */
  boolean isApplicable(@Nonnull PsiElement[] elements);

  /**
   * Performs the Surround With action on the specified range of elements.
   *
   * @param project  the project containing the elements.
   * @param editor   the editor in which the action is invoked.
   * @param elements the elements to be surrounded.
   * @return range to select/to position the caret
   */
  @Nullable
  TextRange surroundElements(@Nonnull Project project,
                             @Nonnull Editor editor,
                             @Nonnull PsiElement[] elements) throws IncorrectOperationException;
}
