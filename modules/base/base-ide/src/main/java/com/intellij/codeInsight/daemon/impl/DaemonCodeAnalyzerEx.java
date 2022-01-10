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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import consulo.logging.Logger;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class DaemonCodeAnalyzerEx extends DaemonCodeAnalyzer {
  private static final Logger LOG = Logger.getInstance(DaemonCodeAnalyzerEx.class);
  public static DaemonCodeAnalyzerEx getInstanceEx(Project project) {
    return (DaemonCodeAnalyzerEx)project.getComponent(DaemonCodeAnalyzer.class);
  }

  /**
   *
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
                                          @javax.annotation.Nullable final HighlightSeverity minSeverity,
                                          final int startOffset,
                                          final int endOffset,
                                          @Nonnull final Processor<HighlightInfo> processor) {
    LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOverlappingWith(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
             || info.highlighter == null
             || processor.process(info);
    });
  }

  /**
   *
   * @param document
   * @param project
   * @param minSeverity null means all
   * @param startOffset
   * @param endOffset
   * @param processor
   * @return
   */
  static boolean processHighlightsOverlappingOutside(@Nonnull Document document,
                                                     @Nonnull Project project,
                                                     @Nullable final HighlightSeverity minSeverity,
                                                     final int startOffset,
                                                     final int endOffset,
                                                     @Nonnull final Processor<HighlightInfo> processor) {
    LOG.assertTrue(ApplicationManager.getApplication().isReadAccessAllowed());

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOutside(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
             || info.highlighter == null
             || processor.process(info);
    });
  }

  static boolean hasErrors(@Nonnull Project project, @Nonnull Document document) {
    return !processHighlights(document, project, HighlightSeverity.ERROR, 0, document.getTextLength(),
                              CommonProcessors.<HighlightInfo>alwaysFalse());
  }

  @Nonnull
  public abstract List<HighlightInfo> runMainPasses(@Nonnull PsiFile psiFile,
                                                    @Nonnull Document document,
                                                    @Nonnull ProgressIndicator progress);

  public abstract boolean isErrorAnalyzingFinished(@Nonnull PsiFile file);

  @Nonnull
  public abstract FileStatusMap getFileStatusMap();

  @Nonnull
  @TestOnly
  public abstract List<HighlightInfo> getFileLevelHighlights(@Nonnull Project project, @Nonnull PsiFile file);

  public abstract void cleanFileLevelHighlights(@Nonnull Project project, int group, PsiFile psiFile);

  public abstract void addFileLevelHighlight(@Nonnull final Project project,
                                             final int group,
                                             @Nonnull final HighlightInfo info,
                                             @Nonnull final PsiFile psiFile);
}
