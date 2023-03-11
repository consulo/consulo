/*
 * Copyright 2013-2023 consulo.io
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
package consulo.sandboxPlugin.lang.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/03/2023
 */
@ExtensionImpl
public class SandLocalInspection extends LocalInspectionTool {
  private static final String SHORT_NAME = getShortName(SandLocalInspection.class);

  private static final class SandClassDisableFix implements LocalQuickFix {

    @Nls
    @Nonnull
    @Override
    public String getName() {
      return "Disable class check";
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "Disable class check";
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
      InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
      profile.<SandLocalInspection, SandLocalInspectionState>modifyToolSettings(SHORT_NAME,
                                                                                descriptor.getPsiElement(), (tool, state) -> {
          state.setCheckClass(false);
        });
    }
  }

  @Nonnull
  @Override
  public String getShortName() {
    return SHORT_NAME;
  }

  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return "General";
  }

  @Nonnull
  @Override
  public InspectionToolState<?> createStateProvider() {
    return new SandLocalInspectionState();
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Test Sand Inspection with Settings";
  }

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder,
                                        boolean isOnTheFly,
                                        @Nonnull LocalInspectionToolSession session,
                                        @Nonnull Object state) {
    SandLocalInspectionState sandState = (SandLocalInspectionState)state;
    return new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof SandClass) {
          boolean checkClass = sandState.isCheckClass();

          if (checkClass) {
            PsiElement nameIdentifier = ((SandClass)element).getNameIdentifier();
            if (nameIdentifier != null) {
              holder.registerProblem(nameIdentifier,
                                     "Test Error",
                                     ProblemHighlightType.ERROR,
                                     new TextRange(0, nameIdentifier.getTextLength()),
                                     new SandClassDisableFix());
            }
          }
        }
      }
    };
  }
}
