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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.document.Document;
import consulo.language.impl.internal.psi.DocumentCommitProcessor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ModalityState;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
@Singleton
public class LightDocumentCommitProcessor implements DocumentCommitProcessor {
  @Override
  public void commitSynchronously(Document document, Project project, PsiFile psiFile) {

  }

  @Override
  public void commitAsynchronously(Project project,
                                   Document document,
                                   Object reason,
                                   ModalityState modalityState) {

  }
}
