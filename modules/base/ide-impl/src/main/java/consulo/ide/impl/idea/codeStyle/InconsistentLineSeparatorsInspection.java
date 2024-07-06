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
package consulo.ide.impl.idea.codeStyle;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.util.lang.StringUtil;
import consulo.language.editor.inspection.*;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author Nikolay Matveev
 */
@ExtensionImpl
public class InconsistentLineSeparatorsInspection extends LocalInspectionTool {
  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return InspectionLocalize.groupNamesPortabilityIssues().get();
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return InspectionLocalize.inconsistentLineSeparators().get();
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitFile(PsiFile file) {
        if (!file.getLanguage().equals(file.getViewProvider().getBaseLanguage())) {
          // There is a possible case that more than a single virtual file/editor contains more than one language (e.g. php and html).
          // We want to process a virtual file once than, hence, ignore all non-base psi files.
          return;
        }

        final Project project = holder.getProject();
        final String projectLineSeparator = CodeStyleFacade.getInstance(project).getLineSeparator();
        if (projectLineSeparator == null) {
          return;
        }

        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || !AbstractConvertLineSeparatorsAction.shouldProcess(virtualFile, project)) {
          return;
        }

        final String curLineSeparator = LoadTextUtil.detectLineSeparator(virtualFile, true);
        if (curLineSeparator != null && !curLineSeparator.equals(projectLineSeparator)) {
          holder.registerProblem(
            file,
            "Line separators in the current file (" + StringUtil.escapeStringCharacters(curLineSeparator) + ") " +
            "differ from the project defaults (" + StringUtil.escapeStringCharacters(projectLineSeparator) + ")",
            SET_PROJECT_LINE_SEPARATORS);
        }
      }
    };
  }

  @Nonnull
  private static final LocalQuickFix SET_PROJECT_LINE_SEPARATORS = new LocalQuickFix() {
    @Nonnull
    @Override
    public String getName() {
      return getFamilyName();
    }

    @Nonnull
    @Override
    public String getFamilyName() {
      return "Convert to project line separators";
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      final PsiElement psiElement = descriptor.getPsiElement();
      if (!(psiElement instanceof PsiFile)) {
        return;
      }

      final String lineSeparator = CodeStyleFacade.getInstance(project).getLineSeparator();
      if (lineSeparator == null) {
        return;
      }

      final VirtualFile virtualFile = ((PsiFile)psiElement).getVirtualFile();
      if (virtualFile != null) {
        AbstractConvertLineSeparatorsAction.changeLineSeparators(project, virtualFile, lineSeparator);
      }
    }
  };
}
