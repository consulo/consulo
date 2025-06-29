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
package consulo.language.editor.intention;

import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author anna
 * @since 2011-10-27
 */
public interface BatchQuickFix<D extends CommonProblemDescriptor> {
  /**
   * Called to apply the cumulative fix. Is invoked in WriteAction
   *
   * @param project    {@link Project}
   * @param descriptors problem reported by the tool on which fix should work
   * @param psiElementsToIgnore elements to be excluded from view during post-refresh
   * @param refreshViews post-refresh inspection results view; would remove collected elements from the view
   */
  void applyFix(@Nonnull final Project project,
                @Nonnull final D[] descriptors,
                final List<PsiElement> psiElementsToIgnore,
                final Runnable refreshViews);
}
