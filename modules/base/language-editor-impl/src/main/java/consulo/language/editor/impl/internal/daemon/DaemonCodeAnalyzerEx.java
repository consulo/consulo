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

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.document.Document;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class DaemonCodeAnalyzerEx extends DaemonCodeAnalyzer {
  private static final Logger LOG = Logger.getInstance(DaemonCodeAnalyzerEx.class);

  public static DaemonCodeAnalyzerEx getInstanceEx(Project project) {
    return (DaemonCodeAnalyzerEx)DaemonCodeAnalyzer.getInstance(project);
  }

  /**
   * @param document
   * @param project
   * @param minSeverity null means all
   * @param startOffset
   * @param endOffset
   * @param processor
   * @return
   */
  public static boolean processHighlights(@Nonnull Document document,
                                          @Nonnull Project project,
                                          @Nullable final HighlightSeverity minSeverity,
                                          final int startOffset,
                                          final int endOffset,
                                          @Nonnull final Processor<HighlightInfo> processor) {
    LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOverlappingWith(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0 || info.getHighlighter() == null || processor.process(info);
    });
  }

  /**
   * @param document
   * @param project
   * @param minSeverity null means all
   * @param startOffset
   * @param endOffset
   * @param processor
   * @return
   */
  public static boolean processHighlightsOverlappingOutside(@Nonnull Document document,
                                                            @Nonnull Project project,
                                                            @Nullable final HighlightSeverity minSeverity,
                                                            final int startOffset,
                                                            final int endOffset,
                                                            @Nonnull final Processor<HighlightInfo> processor) {
    LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOutside(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0 || info.getHighlighter() == null || processor.process(info);
    });
  }

  public static boolean hasErrors(@Nonnull Project project, @Nonnull Document document) {
    return !processHighlights(document, project, HighlightSeverity.ERROR, 0, document.getTextLength(), CommonProcessors.<HighlightInfo>alwaysFalse());
  }

  @Nonnull
  public abstract List<HighlightInfo> runMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document, @Nonnull ProgressIndicator progress);

  public abstract boolean isErrorAnalyzingFinished(@Nonnull PsiFile file);

  @Nonnull
  public abstract FileStatusMap getFileStatusMap();

  @Nonnull
  @TestOnly
  public abstract List<HighlightInfo> getFileLevelHighlights(@Nonnull Project project, @Nonnull PsiFile file);

  public abstract void cleanFileLevelHighlights(@Nonnull Project project, int group, PsiFile psiFile);

  public abstract void addFileLevelHighlight(@Nonnull final Project project, final int group, @Nonnull final HighlightInfo info, @Nonnull final PsiFile psiFile);
}
