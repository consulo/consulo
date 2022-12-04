/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class DaemonCodeAnalyzer {
  @Nonnull
  public static DaemonCodeAnalyzer getInstance(@Nonnull Project project) {
    return project.getComponent(DaemonCodeAnalyzer.class);
  }

  public abstract void settingsChanged();

  public abstract void setUpdateByTimerEnabled(boolean value);

  public abstract void disableUpdateByTimer(@Nonnull Disposable parentDisposable);

  public abstract boolean isHighlightingAvailable(@Nullable PsiFile file);

  public abstract void setImportHintsEnabled(@Nonnull PsiFile file, boolean value);

  public abstract void resetImportHintsEnabledForProject();

  public abstract void setHighlightingEnabled(@Nonnull PsiFile file, boolean value);

  public abstract boolean isImportHintsEnabled(@Nonnull PsiFile file);

  public abstract boolean isAutohintsAvailable(@Nullable PsiFile file);

  @Nonnull
  public abstract ProgressIndicator createDaemonProgressIndicator();

  /**
   * Force rehighlighting for all files.
   */
  public abstract void restart();

  /**
   * Force rehighlighting for a specific file.
   *
   * @param file the file to rehighlight.
   */
  public abstract void restart(@Nonnull PsiFile file);

  public abstract void autoImportReferenceAtCursor(@Nonnull Editor editor, @Nonnull PsiFile file);

  @Nonnull
  public abstract FileStatusMap getFileStatusMap();

  /**
   * @param document
   * @param project
   * @param minSeverity null means all
   * @param startOffset
   * @param endOffset
   * @param processor
   * @return
   */
  @RequiredReadAction
  public static boolean processHighlights(@Nonnull Document document,
                                          @Nonnull Project project,
                                          @Nullable final HighlightSeverity minSeverity,
                                          final int startOffset,
                                          final int endOffset,
                                          @Nonnull final Predicate<HighlightInfo> processor) {
    Application.get().assertReadAccessAllowed();

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOverlappingWith(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0 || info.getHighlighter() == null || processor.test(info);
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
  @RequiredReadAction
  public static boolean processHighlightsOverlappingOutside(@Nonnull Document document,
                                                            @Nonnull Project project,
                                                            @Nullable final HighlightSeverity minSeverity,
                                                            final int startOffset,
                                                            final int endOffset,
                                                            @Nonnull final Predicate<HighlightInfo> processor) {
    Application.get().assertReadAccessAllowed();

    final SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
    MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
    return model.processRangeHighlightersOutside(startOffset, endOffset, marker -> {
      Object tt = marker.getErrorStripeTooltip();
      if (!(tt instanceof HighlightInfo)) return true;
      HighlightInfo info = (HighlightInfo)tt;
      return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0 || info.getHighlighter() == null || processor.test(info);
    });
  }

  @RequiredReadAction
  public static boolean hasErrors(@Nonnull Project project, @Nonnull Document document) {
    return !processHighlights(document, project, HighlightSeverity.ERROR, 0, document.getTextLength(), (i) -> false);
  }
}
