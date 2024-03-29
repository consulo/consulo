/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.application.constraint.ConstrainedExecution;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.ModalityState;

/**
 * from kotlin
 */
public class WithDocumentsCommitted implements ConstrainedExecution.ContextConstraint {
  private final Project myProject;
  private final ModalityState myModalityState;

  public WithDocumentsCommitted(Project project, ModalityState modalityState) {
    myProject = project;
    myModalityState = modalityState;
  }

  @Override
  public boolean isCorrectContext() {
    return !PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }

  @Override
  public void schedule(Runnable runnable) {
    PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(runnable, myModalityState);
  }

  @Override
  public String toString() {
    return "withDocumentsCommitted";
  }
}
