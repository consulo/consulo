/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.QuickFix;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.application.progress.SequentialModalProgressTask;
import consulo.application.progress.SequentialTask;
import jakarta.annotation.Nonnull;

public abstract class PerformFixesModalTask implements SequentialTask {
  @Nonnull
  protected final Project myProject;
  private final CommonProblemDescriptor[] myDescriptors;
  private final SequentialModalProgressTask myTask;
  private final PsiDocumentManager myDocumentManager;
  private int myCount = 0;

  public PerformFixesModalTask(@Nonnull Project project,
                               @Nonnull CommonProblemDescriptor[] descriptors,
                               @Nonnull SequentialModalProgressTask task) {
    myProject = project;
    myDescriptors = descriptors;
    myTask = task;
    myDocumentManager = PsiDocumentManager.getInstance(myProject);
  }

  @Override
  public void prepare() {
  }

  @Override
  public boolean isDone() {
    return myCount > myDescriptors.length - 1;
  }

  @Override
  public boolean iteration() {
    CommonProblemDescriptor descriptor = myDescriptors[myCount++];
    ProgressIndicator indicator = myTask.getIndicator();
    if (indicator != null) {
      indicator.setFraction((double)myCount / myDescriptors.length);
      String presentableText = "usages";
      if (descriptor instanceof ProblemDescriptor) {
        PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
        if (psiElement != null) {
          presentableText = SymbolPresentationUtil.getSymbolPresentableText(psiElement);
        }
      }
      indicator.setText("Processing " + presentableText);
    }

    boolean[] runInReadAction = {false};
    QuickFix[] fixes = descriptor.getFixes();
    if (fixes != null) {
      for (QuickFix fix : fixes) {
        if (!fix.startInWriteAction()) {
          runInReadAction[0] = true;
        } else {
          runInReadAction[0] = false;
          break;
        }
      }
    }

    ApplicationManager.getApplication().runWriteAction(() -> {
      myDocumentManager.commitAllDocuments();
      if (!runInReadAction[0]) {
        applyFix(myProject, descriptor);
      }
    });
    if (runInReadAction[0]) {
      applyFix(myProject, descriptor);
    }
    return isDone();
  }

  @Override
  public void stop() {}

  protected abstract void applyFix(Project project, CommonProblemDescriptor descriptor);
}
