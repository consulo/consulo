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
package consulo.language.editor.impl.internal.highlight;

import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.FileStatusMap;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author cdr
 */
public abstract class ProgressableTextEditorHighlightingPass extends TextEditorHighlightingPass {
  private volatile boolean myFinished;
  private volatile long myProgressLimit;
  private final AtomicLong myProgressCount = new AtomicLong();
  private volatile long myNextChunkThreshold; // the value myProgressCount should exceed to generate next fireProgressAdvanced event
  private final String myPresentableName;
  protected final PsiFile myFile;
  @Nullable
  private final Editor myEditor;
  @Nonnull
  protected final TextRange myRestrictRange;
  @Nonnull
  protected final HighlightInfoProcessor myHighlightInfoProcessor;
  protected HighlightingSession myHighlightingSession;

  protected ProgressableTextEditorHighlightingPass(@Nonnull Project project,
                                                   @Nullable final Document document,
                                                   @Nonnull String presentableName,
                                                   @Nullable PsiFile file,
                                                   @Nullable Editor editor,
                                                   @Nonnull TextRange restrictRange,
                                                   boolean runIntentionPassAfter,
                                                   @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    super(project, document, runIntentionPassAfter);
    myPresentableName = presentableName;
    myFile = file;
    myEditor = editor;
    myRestrictRange = restrictRange;
    myHighlightInfoProcessor = highlightInfoProcessor;
  }

  @Override
  protected boolean isValid() {
    return super.isValid() && (myFile == null || myFile.isValid());
  }

  private void sessionFinished() {
    advanceProgress(Math.max(1, myProgressLimit - myProgressCount.get()));
  }

  @Override
  public final void doCollectInformation(@Nonnull final ProgressIndicator progress) {
    if (!(progress instanceof DaemonProgressIndicator)) {
      throw new IncorrectOperationException("Highlighting must be run under DaemonProgressIndicator, but got: " + progress);
    }
    myFinished = false;
    if (myFile != null) {
      myHighlightingSession = HighlightingSessionImpl.getOrCreateHighlightingSession(myFile, (DaemonProgressIndicator)progress, getColorsScheme());
    }
    try {
      collectInformationWithProgress(progress);
    }
    finally {
      if (myFile != null) {
        sessionFinished();
      }
    }
  }

  protected abstract void collectInformationWithProgress(@Nonnull ProgressIndicator progress);

  @Override
  public final void doApplyInformationToEditor() {
    myFinished = true;
    applyInformationWithProgress();
    DaemonCodeAnalyzerInternal daemonCodeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(myProject);
    daemonCodeAnalyzer.getFileStatusMap().markFileUpToDate(myDocument, getId());
  }

  protected abstract void applyInformationWithProgress();

  /**
   * @return number in the [0..1] range;
   * <0 means progress is not available
   */
  public double getProgress() {
    long progressLimit = getProgressLimit();
    if (progressLimit == 0) return -1;
    long progressCount = getProgressCount();
    return progressCount > progressLimit ? 1 : (double)progressCount / progressLimit;
  }

  private long getProgressLimit() {
    return myProgressLimit;
  }

  private long getProgressCount() {
    return myProgressCount.get();
  }

  public boolean isFinished() {
    return myFinished;
  }

  // null means do not show progress
  @Nullable
  public String getPresentableName() {
    return myPresentableName;
  }

  protected Editor getEditor() {
    return myEditor;
  }

  public void setProgressLimit(long limit) {
    myProgressLimit = limit;
    myNextChunkThreshold = Math.max(1, limit / 100); // 1% precision
  }

  public void advanceProgress(long delta) {
    if (myHighlightingSession != null) {
      // session can be null in e.g. inspection batch mode
      long current = myProgressCount.addAndGet(delta);
      if (current >= myNextChunkThreshold) {
        double progress = getProgress();
        myNextChunkThreshold += Math.max(1, myProgressLimit / 100);
        myHighlightInfoProcessor.progressIsAdvanced(myHighlightingSession, getEditor(), progress);
      }
    }
  }

  @RequiredUIAccess
  void waitForHighlightInfosApplied() {
    UIAccess.assertIsUIThread();
    HighlightingSessionImpl session = (HighlightingSessionImpl)myHighlightingSession;
    if (session != null) {
      session.waitForHighlightInfosApplied();
    }
  }

  public static class EmptyPass extends TextEditorHighlightingPass {
    public EmptyPass(final Project project, @Nullable final Document document) {
      super(project, document, false);
    }

    @Override
    public void doCollectInformation(@Nonnull final ProgressIndicator progress) {
    }

    @Override
    public void doApplyInformationToEditor() {
      FileStatusMap statusMap = DaemonCodeAnalyzerInternal.getInstanceEx(myProject).getFileStatusMap();
      statusMap.markFileUpToDate(getDocument(), getId());
    }
  }
}
