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
package consulo.language.editor.impl.internal.daemon;

import consulo.application.progress.ProgressIndicator;
import consulo.document.Document;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import java.util.List;

public abstract class DaemonCodeAnalyzerEx extends DaemonCodeAnalyzer {
  private static final Logger LOG = Logger.getInstance(DaemonCodeAnalyzerEx.class);

  public static DaemonCodeAnalyzerEx getInstanceEx(Project project) {
    return (DaemonCodeAnalyzerEx)DaemonCodeAnalyzer.getInstance(project);
  }

  @Nonnull
  public abstract List<HighlightInfo> runMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document, @Nonnull ProgressIndicator progress);

  public abstract boolean isErrorAnalyzingFinished(@Nonnull PsiFile file);

  @Nonnull
  public abstract FileStatusMapImpl getFileStatusMap();

  @Nonnull
  @TestOnly
  public abstract List<HighlightInfo> getFileLevelHighlights(@Nonnull Project project, @Nonnull PsiFile file);

  public abstract void cleanFileLevelHighlights(@Nonnull Project project, int group, PsiFile psiFile);

  public abstract void addFileLevelHighlight(@Nonnull final Project project, final int group, @Nonnull final HighlightInfo info, @Nonnull final PsiFile psiFile);
}
