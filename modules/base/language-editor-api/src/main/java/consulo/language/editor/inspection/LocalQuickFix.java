/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.component.util.Iconable;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.util.collection.ArrayFactory;

/**
 * QuickFix based on {@link ProblemDescriptor ProblemDescriptor}
 * <p/>
 * N.B. Please DO NOT store PSI elements inside the LocalQuickFix instance, to avoid holding too much PSI files during inspection.
 * Instead, use the {@link ProblemDescriptor#getPsiElement()}
 * in {@link QuickFix#applyFix(Project, CommonProblemDescriptor)}
 * to retrieve the PSI context the fix will work on.
 * See also {@link LocalQuickFixOnPsiElement} which uses {@link SmartPsiElementPointer} instead of storing PSI elements.
 * <p/>
 * Implement {@link Iconable Iconable} interface to
 * change icon in quick fix popup menu.
 * <p/>
 * Implement {@link consulo.ide.impl.idea.codeInsight.intention.HighPriorityAction HighPriorityAction} or
 * {@link consulo.ide.impl.idea.codeInsight.intention.LowPriorityAction LowPriorityAction} to change ordering.
 *
 * @author max
 * @see LocalQuickFixBase
 * @see ProblemDescriptor
 * @see Iconable
 */
public interface LocalQuickFix extends QuickFix<ProblemDescriptor>, FileModifier {
  LocalQuickFix[] EMPTY_ARRAY = new LocalQuickFix[0];
  
  ArrayFactory<LocalQuickFix> ARRAY_FACTORY = i -> i == 0 ? EMPTY_ARRAY : new LocalQuickFix[i];
}
