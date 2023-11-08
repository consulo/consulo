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
package consulo.language.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2023-11-08
 */
public interface PsiSearchHelperEx extends PsiSearchHelper {
  boolean processFilesConcurrentlyDespiteWriteActions(@Nonnull Project project,
                                                      @Nonnull List<? extends VirtualFile> files,
                                                      @Nonnull final ProgressIndicator progress,
                                                      @Nonnull AtomicBoolean stopped,
                                                      @Nonnull final Predicate<? super VirtualFile> localProcessor);
}
