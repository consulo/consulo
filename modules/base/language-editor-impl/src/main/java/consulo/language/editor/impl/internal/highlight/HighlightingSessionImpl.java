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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HighlightingSessionImpl implements HighlightingSession {
  private final PsiFile myPsiFile;
  private final ProgressIndicator myProgressIndicator;
  private final EditorColorsScheme myEditorColorsScheme;
  private final Project myProject;
  private final Document myDocument;
  private final ProperTextRange myVisibleRange;
  private final Map<TextRange, RangeMarker> myRanges2markersCache = new HashMap<>();

  private HighlightingSessionImpl(
      PsiFile psiFile,
      DaemonProgressIndicator progressIndicator,
      EditorColorsScheme editorColorsScheme,
      ProperTextRange visibleRange
  ) {
    myPsiFile = psiFile;
    myProgressIndicator = progressIndicator;
    myEditorColorsScheme = editorColorsScheme;
    myProject = psiFile.getProject();
    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
    myVisibleRange = visibleRange;
  }

  private static final Key<ConcurrentMap<PsiFile, HighlightingSession>> HIGHLIGHTING_SESSION = Key.create("HIGHLIGHTING_SESSION");

  public static HighlightingSession getHighlightingSession(PsiFile psiFile, ProgressIndicator progressIndicator) {
    Map<PsiFile, HighlightingSession> map = ((DaemonProgressIndicator)progressIndicator).getUserData(HIGHLIGHTING_SESSION);
    return map == null ? null : map.get(psiFile);
  }

  public static HighlightingSession getOrCreateHighlightingSession(
      PsiFile psiFile,
      DaemonProgressIndicator progressIndicator,
      @Nullable EditorColorsScheme editorColorsScheme,
      ProperTextRange visibleRange
  ) {
    HighlightingSession session = getHighlightingSession(psiFile, progressIndicator);
    if (session == null) {
      ConcurrentMap<PsiFile, HighlightingSession> map = progressIndicator.getUserData(HIGHLIGHTING_SESSION);
      if (map == null) {
        map = progressIndicator.putUserDataIfAbsent(HIGHLIGHTING_SESSION, new ConcurrentHashMap<>());
      }
      session = Maps.cacheOrGet(map, psiFile,
          new HighlightingSessionImpl(psiFile, progressIndicator, editorColorsScheme, visibleRange));
    }
    return session;
  }

  /**
   * Retrieves the HighlightingSession for the given PsiFile from the current DaemonProgressIndicator.
   * Must be called from a thread running under a DaemonProgressIndicator
   * (via {@link ProgressManager#executeProcessUnderProgress}).
   *
   * <p>This follows the JetBrains pattern where the session (with pre-computed visible range)
   * is created on EDT and stored on the DaemonProgressIndicator, then factories
   * retrieve it on the background thread.</p>
   */
  public static HighlightingSession getFromCurrentIndicator(PsiFile psiFile) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (!(indicator instanceof DaemonProgressIndicator dpi)) {
      throw new IllegalStateException("Must be run under DaemonProgressIndicator, but got: " + indicator);
    }
    HighlightingSession session = getHighlightingSession(psiFile, dpi);
    if (session == null) {
      throw new IllegalStateException("No HighlightingSession for " + psiFile + " in " + indicator);
    }
    return session;
  }

  public static void waitForAllSessionsHighlightInfosApplied(DaemonProgressIndicator progressIndicator) {
    ConcurrentMap<PsiFile, HighlightingSession> map = progressIndicator.getUserData(HIGHLIGHTING_SESSION);
    if (map != null) {
      for (HighlightingSession session : map.values()) {
        ((HighlightingSessionImpl)session).waitForHighlightInfosApplied();
      }
    }
  }


  @Override
  public PsiFile getPsiFile() {
    return myPsiFile;
  }

  @Override
  public Document getDocument() {
    return myDocument;
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return myProgressIndicator;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public EditorColorsScheme getColorsScheme() {
    return myEditorColorsScheme;
  }

  @Override
  public ProperTextRange getVisibleRange() {
    return myVisibleRange;
  }

  @RequiredReadAction
  public void applyHighlightInfo(HighlightInfo info, TextRange restrictedRange, int groupId) {
    EditorColorsScheme colorsScheme = getColorsScheme();
    UpdateHighlightersUtilImpl.addHighlighterToEditorIncrementally(
        myProject, getDocument(), getPsiFile(),
        restrictedRange.getStartOffset(), restrictedRange.getEndOffset(),
        (HighlightInfoImpl)info, colorsScheme, groupId, myRanges2markersCache);
  }

  @RequiredReadAction
  public void disposeHighlighterFor(HighlightInfo info) {
    RangeHighlighterEx highlighter = ((HighlightInfoImpl)info).getHighlighter();
    if (highlighter == null) return;
    // that highlighter may have been reused for another info
    Object actualInfo = highlighter.getErrorStripeTooltip();
    if (actualInfo == info && info.getHighlighter() == highlighter) {
      highlighter.dispose();
    }
  }

  /**
   * No-op: highlights are now applied directly from background thread under read lock.
   * Kept for API compatibility with {@link #waitForAllSessionsHighlightInfosApplied}.
   */
  public void waitForHighlightInfosApplied() {
  }

  public static void clearProgressIndicator(DaemonProgressIndicator indicator) {
    indicator.putUserData(HIGHLIGHTING_SESSION, null);
  }
}
