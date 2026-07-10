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
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.HighlightingSession;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
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
  private final Map<TextRange, RangeMarker> myRanges2markersCache = new HashMap<>();
  private final Map<Class<?>, Object> myUIContexts;

  private HighlightingSessionImpl(PsiFile psiFile, DaemonProgressIndicator progressIndicator, EditorColorsScheme editorColorsScheme, @Nullable Map<Class<?>, Object> uiContexts) {
    myPsiFile = psiFile;
    myProgressIndicator = progressIndicator;
    myEditorColorsScheme = editorColorsScheme;
    myUIContexts = uiContexts != null && !uiContexts.isEmpty() ? new HashMap<>(uiContexts) : Map.of();
    myProject = psiFile.getProject();
    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
  }

  private static final Key<ConcurrentMap<PsiFile, HighlightingSession>> HIGHLIGHTING_SESSION = Key.create("HIGHLIGHTING_SESSION");

  public static HighlightingSession getHighlightingSession(PsiFile psiFile, ProgressIndicator progressIndicator) {
    Map<PsiFile, HighlightingSession> map = ((DaemonProgressIndicator)progressIndicator).getUserData(HIGHLIGHTING_SESSION);
    return map == null ? null : map.get(psiFile);
  }

  
  public static HighlightingSession getOrCreateHighlightingSession(PsiFile psiFile, DaemonProgressIndicator progressIndicator, @Nullable EditorColorsScheme editorColorsScheme) {
    return getOrCreateHighlightingSession(psiFile, progressIndicator, editorColorsScheme, Map.of());
  }

  public static HighlightingSession getOrCreateHighlightingSession(PsiFile psiFile,
                                                                   DaemonProgressIndicator progressIndicator,
                                                                   @Nullable EditorColorsScheme editorColorsScheme,
                                                                   @Nullable Map<Class<?>, Object> uiContexts) {
    HighlightingSession session = getHighlightingSession(psiFile, progressIndicator);
    if (session == null) {
      ConcurrentMap<PsiFile, HighlightingSession> map = progressIndicator.getUserData(HIGHLIGHTING_SESSION);
      if (map == null) {
        map = progressIndicator.putUserDataIfAbsent(HIGHLIGHTING_SESSION, new ConcurrentHashMap<>());
      }
      session = Maps.cacheOrGet(map, psiFile, new HighlightingSessionImpl(psiFile, progressIndicator, editorColorsScheme, uiContexts));
    }
    return session;
  }

  /**
   * Retrieves the {@link HighlightingSession} for the given file from the current
   * {@link DaemonProgressIndicator}. The session (with any pre-captured EDT UI contexts)
   * must have been created earlier in the highlighting cycle.
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

  @Override
  public @Nullable Object getUIContext(Class<?> factoryClass) {
    return myUIContexts.get(factoryClass);
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

  @RequiredReadAction
  public void applyHighlightInfo(HighlightInfo info, TextRange restrictedRange, int groupId) {
    EditorColorsScheme colorsScheme = getColorsScheme();
    UpdateHighlightersUtilImpl
            .addHighlighterToEditorIncrementally(myProject, getDocument(), getPsiFile(), restrictedRange.getStartOffset(), restrictedRange.getEndOffset(), (HighlightInfoImpl)info, colorsScheme,
                                                 groupId, myRanges2markersCache);
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
