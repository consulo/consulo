/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.impl.internal.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.Document;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.APPLICATION)
public interface DocumentCommitProcessor {
  void commitSynchronously(@Nonnull Document document, @Nonnull Project project, @Nonnull PsiFile psiFile);

  void commitAsynchronously(@Nonnull Project project,
                            @Nonnull Document document,
                            @Nonnull Object reason,
                            @Nonnull ModalityState modalityState);
}
