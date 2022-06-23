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
package consulo.ide.impl.idea.execution.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunManager;
import consulo.ide.impl.idea.execution.configurations.RefactoringListenerProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.logging.Logger;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.ide.impl.idea.refactoring.listeners.RefactoringElementListenerComposite;
import consulo.language.editor.refactoring.event.RefactoringElementListenerProvider;

/**
 * @author spleaner
*/
@ExtensionImpl
public class RunConfigurationRefactoringElementListenerProvider implements RefactoringElementListenerProvider {
  private static final Logger LOG = Logger.getInstance(RunConfigurationRefactoringElementListenerProvider.class);

  @Override
  public RefactoringElementListener getListener(final PsiElement element) {
    RefactoringElementListenerComposite composite = null;
    final RunConfiguration[] configurations = RunManager.getInstance(element.getProject()).getAllConfigurations();

    for (RunConfiguration configuration : configurations) {
      if (configuration instanceof RefactoringListenerProvider) { // todo: perhaps better way to handle listeners?
        final RefactoringElementListener listener;
        try {
          listener = ((RefactoringListenerProvider)configuration).getRefactoringElementListener(element);
        }
        catch (Exception e) {
          LOG.error(e);
          continue;
        }
        if (listener != null) {
          if (composite == null) {
            composite = new RefactoringElementListenerComposite();
          }
          composite.addListener(listener);
        }
      }
    }
    return composite;
  }
}
