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

package consulo.language.editor.wolfAnalyzer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author cdr
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class WolfTheProblemSolver {
  public static WolfTheProblemSolver getInstance(Project project) {
    return project.getInstance(WolfTheProblemSolver.class);
  }

  public abstract boolean isProblemFile(VirtualFile virtualFile);

  public abstract void weHaveGotProblems(@Nonnull VirtualFile virtualFile, @Nonnull List<Problem> problems);

  public abstract void weHaveGotNonIgnorableProblems(@Nonnull VirtualFile virtualFile, @Nonnull List<Problem> problems);

  public abstract void clearProblems(@Nonnull VirtualFile virtualFile);

  public abstract boolean hasProblemFilesBeneath(@Nonnull Predicate<VirtualFile> condition);

  public abstract boolean hasProblemFilesBeneath(@Nonnull Module scope);

  public abstract Problem convertToProblem(VirtualFile virtualFile, int line, int column, String[] message);

  public abstract void reportProblems(final VirtualFile file, Collection<Problem> problems);

  public abstract boolean hasSyntaxErrors(final VirtualFile file);

  @Deprecated
  public abstract static class ProblemListener implements consulo.language.editor.wolfAnalyzer.ProblemListener {
    @Override
    public void problemsAppeared(@Nonnull VirtualFile file) {
    }

    @Override
    public void problemsDisappeared(@Nonnull VirtualFile file) {
    }
  }

  /**
   * @deprecated Use message bus {@link consulo.language.editor.wolfAnalyzer.ProblemListener#TOPIC} instead.
   */
  @Deprecated
  public abstract void addProblemListener(@Nonnull ProblemListener listener, @Nonnull Disposable parentDisposable);

  public abstract void queue(VirtualFile suspiciousFile);
}
