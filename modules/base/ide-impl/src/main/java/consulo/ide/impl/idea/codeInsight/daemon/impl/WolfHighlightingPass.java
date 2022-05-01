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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.DaemonBundle;
import consulo.ide.impl.idea.codeInsight.problems.WolfTheProblemSolverImpl;
import consulo.document.Document;
import consulo.application.progress.ProgressIndicator;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.application.util.registry.Registry;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;

/**
 * @author cdr
 */
class WolfHighlightingPass extends ProgressableTextEditorHighlightingPass implements DumbAware {
  WolfHighlightingPass(@Nonnull Project project, @Nonnull Document document, @Nonnull PsiFile file) {
    super(project, document, DaemonBundle.message("pass.wolf"), file, null, TextRange.EMPTY_RANGE, false, HighlightInfoProcessor.getEmpty());
  }

  @Override
  protected void collectInformationWithProgress(@Nonnull final ProgressIndicator progress) {
    if (!Registry.is("wolf.the.problem.solver", true)) return;
    final WolfTheProblemSolver solver = WolfTheProblemSolver.getInstance(myProject);
    if (solver instanceof WolfTheProblemSolverImpl) {
      ((WolfTheProblemSolverImpl)solver).startCheckingIfVincentSolvedProblemsYet(progress, this);
    }
  }

  @Override
  protected void applyInformationWithProgress() {

  }
}
