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
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.impl.internal.daemon.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HighlightingSessionImpl implements HighlightingSession {
  @Nonnull
  private final PsiFile myPsiFile;
  @Nonnull
  private final ProgressIndicator myProgressIndicator;
  private final EditorColorsScheme myEditorColorsScheme;
  @Nonnull
  private final Project myProject;
  private final Document myDocument;
  private final Map<TextRange, RangeMarker> myRanges2markersCache = new HashMap<>();

  private HighlightingSessionImpl(@Nonnull PsiFile psiFile, @Nonnull DaemonProgressIndicator progressIndicator, EditorColorsScheme editorColorsScheme) {
    myPsiFile = psiFile;
    myProgressIndicator = progressIndicator;
    myEditorColorsScheme = editorColorsScheme;
    myProject = psiFile.getProject();
    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
  }

  private static final Key<ConcurrentMap<PsiFile, HighlightingSession>> HIGHLIGHTING_SESSION = Key.create("HIGHLIGHTING_SESSION");

  @Deprecated
  void applyInEDT(@RequiredUIAccess  @Nonnull Runnable runnable) {
    myProject.getUIAccess().giveIfNeed(runnable);
  }

  public static HighlightingSession getHighlightingSession(@Nonnull PsiFile psiFile, @Nonnull ProgressIndicator progressIndicator) {
    Map<PsiFile, HighlightingSession> map = ((DaemonProgressIndicator)progressIndicator).getUserData(HIGHLIGHTING_SESSION);
    return map == null ? null : map.get(psiFile);
  }

  @Nonnull
  public static HighlightingSession getOrCreateHighlightingSession(@Nonnull PsiFile psiFile, @Nonnull DaemonProgressIndicator progressIndicator, @Nullable EditorColorsScheme editorColorsScheme) {
    HighlightingSession session = getHighlightingSession(psiFile, progressIndicator);
    if (session == null) {
      ConcurrentMap<PsiFile, HighlightingSession> map = progressIndicator.getUserData(HIGHLIGHTING_SESSION);
      if (map == null) {
        map = progressIndicator.putUserDataIfAbsent(HIGHLIGHTING_SESSION, new ConcurrentHashMap<>());
      }
      session = Maps.cacheOrGet(map, psiFile, new HighlightingSessionImpl(psiFile, progressIndicator, editorColorsScheme));
    }
    return session;
  }

  public static void waitForAllSessionsHighlightInfosApplied(@Nonnull DaemonProgressIndicator progressIndicator) {
    ConcurrentMap<PsiFile, HighlightingSession> map = progressIndicator.getUserData(HIGHLIGHTING_SESSION);
    if (map != null) {
      for (HighlightingSession session : map.values()) {
        ((HighlightingSessionImpl)session).waitForHighlightInfosApplied();
      }
    }
  }


  @Nonnull
  @Override
  public PsiFile getPsiFile() {
    return myPsiFile;
  }

  @Nonnull
  @Override
  public Document getDocument() {
    return myDocument;
  }

  @Nonnull
  @Override
  public ProgressIndicator getProgressIndicator() {
    return myProgressIndicator;
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public EditorColorsScheme getColorsScheme() {
    return myEditorColorsScheme;
  }

  public void queueHighlightInfo(@Nonnull HighlightInfo info, @Nonnull TextRange restrictedRange, int groupId) {
    applyInEDT(() -> {
      final EditorColorsScheme colorsScheme = getColorsScheme();
      UpdateHighlightersUtilImpl
              .addHighlighterToEditorIncrementally(myProject, getDocument(), getPsiFile(), restrictedRange.getStartOffset(), restrictedRange.getEndOffset(), (HighlightInfoImpl)info, colorsScheme,
                                                   groupId, myRanges2markersCache);
    });
  }

  public void queueDisposeHighlighterFor(@Nonnull HighlightInfo info) {
    RangeHighlighterEx highlighter = ((HighlightInfoImpl)info).getHighlighter();
    if (highlighter == null) return;
    // that highlighter may have been reused for another info
    applyInEDT(() -> {
      Object actualInfo = highlighter.getErrorStripeTooltip();
      if (actualInfo == info && info.getHighlighter() == highlighter) highlighter.dispose();
    });
  }

  public void waitForHighlightInfosApplied() {
  }

  public static void clearProgressIndicator(@Nonnull DaemonProgressIndicator indicator) {
    indicator.putUserData(HIGHLIGHTING_SESSION, null);
  }
}
