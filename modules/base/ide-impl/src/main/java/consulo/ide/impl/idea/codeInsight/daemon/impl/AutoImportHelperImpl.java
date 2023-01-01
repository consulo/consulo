/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.LaterInvocator;
import consulo.ide.impl.idea.codeInsight.actions.OptimizeImportsProcessor;
import consulo.language.editor.AutoImportHelper;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.awt.internal.ModalityPerProjectEAPDescriptor;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26-Jul-22
 */
@Singleton
@ServiceImpl
public class AutoImportHelperImpl implements AutoImportHelper {
  @Override
  public boolean canChangeFileSilently(@Nonnull PsiFile file) {
    return DaemonListeners.canChangeFileSilently(file);
  }

  @Override
  public boolean mayAutoImportNow(@Nonnull PsiFile psiFile, boolean isInContent) {
    Project project = psiFile.getProject();
    boolean isInModlessContext = ModalityPerProjectEAPDescriptor.is() ? !LaterInvocator.isInModalContextForProject(project) : !LaterInvocator.isInModalContext();
    return isInModlessContext && canChangeFileSilently(psiFile);
  }

  @Override
  public void runOptimizeImports(@Nonnull Project project, @Nonnull PsiFile file, boolean withProgress) {
    OptimizeImportsProcessor processor = new OptimizeImportsProcessor(project, file);
    if (withProgress) {
      processor.run();
    }
    else {
      processor.runWithoutProgress();
    }
  }
}
