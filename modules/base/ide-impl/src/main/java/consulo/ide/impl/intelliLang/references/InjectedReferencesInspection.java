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
package consulo.ide.impl.intelliLang.references;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 *         Date: 05.08.13
 */
@ExtensionImpl
public class InjectedReferencesInspection extends LocalInspectionTool {

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {

        PsiReference[] injected = InjectedReferencesContributor.getInjectedReferences(element);
        if (injected != null) {
          for (PsiReference reference : injected) {
            if (reference.resolve() == null) {
              holder.registerProblem(reference);
            }
          }
        }

        super.visitElement(element);
      }
    };
  }

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return InspectionsBundle.message("inspection.general.tools.group.name");
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Injected References";
  }
}
