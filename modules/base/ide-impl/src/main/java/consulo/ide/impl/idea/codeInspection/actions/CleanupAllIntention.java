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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CleanupAllIntention extends CleanupIntention {
  public static final CleanupAllIntention INSTANCE = new CleanupAllIntention();

  private CleanupAllIntention() {}

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return InspectionLocalize.cleanupInFile();
  }

  @Nullable
  @Override
  protected AnalysisScope getScope(Project project, PsiFile file) {
    return new AnalysisScope(file);
  }
}
