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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.constraints.ConstrainedExecution;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;

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
