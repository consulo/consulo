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
package consulo.externalSystem.task;

import consulo.execution.action.PsiLocation;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 6/5/13 8:11 PM
 */
public class ExternalSystemTaskLocation extends PsiLocation<PsiFile> {

  @Nonnull
  private final ExternalTaskExecutionInfo myTaskInfo;

  public ExternalSystemTaskLocation(@Nonnull Project project, @Nonnull PsiFile psiElement, @Nonnull ExternalTaskExecutionInfo taskInfo) {
    super(project, psiElement);
    myTaskInfo = taskInfo;
  }

  @Nonnull
  public ExternalTaskExecutionInfo getTaskInfo() {
    return myTaskInfo;
  }
}
