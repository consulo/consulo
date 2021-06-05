// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassManager;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public final class FileStatusMap implements Disposable {
  private static final Logger LOG = Logger.getInstance(FileStatusMap.class);
  public static final String CHANGES_NOT_ALLOWED_DURING_HIGHLIGHTING = "PSI/document/model changes are not allowed during highlighting";
  private final Project myProject;
  private final Map<Document, FileStatus> myDocumentToStatusMap = ContainerUtil.createWeakMap(); // all dirty if absent
  private volatile boolean myAllowDirt = true;

  // Don't reduce visibility rules here because this class is used in Upsource as well.
  public FileStatusMap(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
    // clear dangling references to PsiFiles/Documents. SCR#10358
    markAllFilesDirty("FileStatusMap dispose");
  }

  /**
   * @param editor
   * @param passId
   * @return null means the file is clean
   */
  @Nullable
  // used in scala
  public static TextRange getDirtyTextRange(@Nonnull Editor editor, int passId) {
    Document document = editor.getDocument();

    FileStatusMap me = DaemonCodeAnalyzerEx.getInstanceEx(editor.getProject()).getFileStatusMap();
    TextRange dirtyScope = me.getFileDirtyScope(document, passId);
    if (dirtyScope == null) return null;
    TextRange documentRange = TextRange.from(0, document.getTextLength());
    return documentRange.intersection(dirtyScope);
  }

  public void setErrorFoundFlag(@Nonnull Project project, @Nonnull Document document, boolean errorFound) {
    //GHP has found error. Flag is used by ExternalToolPass to decide whether to run or not
    synchronized (myDocumentToStatusMap) {
      FileStatus status = myDocumentToStatusMap.get(document);
      if (status == null) {
        if (!errorFound) return;
        status = new FileStatus(project);
        myDocumentToStatusMap.put(document, status);
      }
      status.errorFound = errorFound;
    }
  }

  boolean wasErrorFound(@Nonnull Document document) {
    synchronized (myDocumentToStatusMap) {
      FileStatus status = myDocumentToStatusMap.get(document);
      return status != null && status.errorFound;
    }
  }

  private static class FileStatus {
    private boolean defensivelyMarked; // file marked dirty without knowledge of specific dirty region. Subsequent markScopeDirty can refine dirty scope, not extend it
    private boolean wolfPassFinished;
    // if contains the special value "WHOLE_FILE_MARKER" then the corresponding range is (0, document length)
    private final IntObjectMap<RangeMarker> dirtyScopes = IntMaps.newIntObjectHashMap();
    private boolean errorFound;

    private FileStatus(@Nonnull Project project) {
      markWholeFileDirty(project);
    }

    private void markWholeFileDirty(@Nonnull Project project) {
      setDirtyScope(Pass.UPDATE_ALL, WHOLE_FILE_DIRTY_MARKER);
      setDirtyScope(Pass.EXTERNAL_TOOLS, WHOLE_FILE_DIRTY_MARKER);
      setDirtyScope(Pass.LOCAL_INSPECTIONS, WHOLE_FILE_DIRTY_MARKER);
      setDirtyScope(Pass.LINE_MARKERS, WHOLE_FILE_DIRTY_MARKER);
      TextEditorHighlightingPassManager registrar = TextEditorHighlightingPassManager.getInstance(project);
      for (DirtyScopeTrackingHighlightingPassFactory factory : registrar.getDirtyScopeTrackingFactories()) {
        setDirtyScope(factory.getPassId(), WHOLE_FILE_DIRTY_MARKER);
      }
    }

    private boolean allDirtyScopesAreNull() {
      for (Object o : dirtyScopes.values()) {
        if (o != null) return false;
      }
      return true;
    }

    private void combineScopesWith(@Nonnull final TextRange scope, final int fileLength, @Nonnull final Document document) {
      List<IntObjectMap.IntObjectEntry<RangeMarker>> rangeMarkers = new ArrayList<>(dirtyScopes.entrySet());

      for (IntObjectMap.IntObjectEntry<RangeMarker> entry : rangeMarkers) {
        int key = entry.getKey();
        RangeMarker oldScope = entry.getValue();

        RangeMarker newScope = combineScopes(oldScope, scope, fileLength, document);
        if (newScope != oldScope && oldScope != null) {
          oldScope.dispose();
        }

        dirtyScopes.put(key, newScope);
      }
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder();
      s.append("defensivelyMarked = ").append(defensivelyMarked);
      s.append("; wolfPassFinfished = ").append(wolfPassFinished);
      s.append("; errorFound = ").append(errorFound);
      s.append("; dirtyScopes: (");
      for (IntObjectMap.IntObjectEntry<RangeMarker> entry : dirtyScopes.entrySet()) {
        int passId = entry.getKey();
        RangeMarker rangeMarker = entry.getValue();
        s.append(" pass: ").append(passId).append(" -> ").append(rangeMarker == WHOLE_FILE_DIRTY_MARKER ? "Whole file" : rangeMarker).append(";");
      }
      s.append(")");
      return s.toString();
    }

    private void setDirtyScope(int passId, RangeMarker scope) {
      RangeMarker marker = dirtyScopes.get(passId);
      if (marker != scope) {
        if (marker != null) {
          marker.dispose();
        }
        dirtyScopes.put(passId, scope);
      }
    }
  }

  void markAllFilesDirty(@Nonnull Object reason) {
    assertAllowModifications();
    synchronized (myDocumentToStatusMap) {
      if (!myDocumentToStatusMap.isEmpty()) {
        log("Mark all dirty: ", reason);
      }
      myDocumentToStatusMap.clear();
    }
  }

  private void assertAllowModifications() {
    try {
      assert myAllowDirt : CHANGES_NOT_ALLOWED_DURING_HIGHLIGHTING;
    }
    finally {
      myAllowDirt = true; //give next test a chance
    }
  }

  public void markFileUpToDate(@Nonnull Document document, int passId) {
    synchronized (myDocumentToStatusMap) {
      FileStatus status = myDocumentToStatusMap.computeIfAbsent(document, __ -> new FileStatus(myProject));
      status.defensivelyMarked = false;
      if (passId == Pass.WOLF) {
        status.wolfPassFinished = true;
      }
      else if (status.dirtyScopes.containsKey(passId)) {
        status.setDirtyScope(passId, null);
      }
    }
  }

  /**
   * @return null for processed file, whole file for untouched or entirely dirty file, range(usually code block) for dirty region (optimization)
   */
  @Nullable
  public TextRange getFileDirtyScope(@Nonnull Document document, int passId) {
    synchronized (myDocumentToStatusMap) {
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
      if (!ProblemHighlightFilter.shouldHighlightFile(file)) return null;
      FileStatus status = myDocumentToStatusMap.get(document);
      if (status == null) {
        return file == null ? null : file.getTextRange();
      }
      if (status.defensivelyMarked) {
        status.markWholeFileDirty(myProject);
        status.defensivelyMarked = false;
      }
      if (!status.dirtyScopes.containsKey(passId)) throw new IllegalStateException("Unknown pass " + passId);
      RangeMarker marker = status.dirtyScopes.get(passId);
      return marker == null ? null : marker.isValid() ? TextRange.create(marker) : new TextRange(0, document.getTextLength());
    }
  }

  void markFileScopeDirtyDefensively(@Nonnull PsiFile file, @Nonnull Object reason) {
    assertAllowModifications();
    log("Mark dirty file defensively: ", file.getName(), reason);
    // mark whole file dirty in case no subsequent PSI events will come, but file requires rehighlighting nevertheless
    // e.g. in the case of quick typing/backspacing char
    synchronized (myDocumentToStatusMap) {
      Document document = PsiDocumentManager.getInstance(myProject).getCachedDocument(file);
      if (document == null) return;
      FileStatus status = myDocumentToStatusMap.get(document);
      if (status == null) return; // all dirty already
      status.defensivelyMarked = true;
    }
  }

  void markFileScopeDirty(@Nonnull Document document, @Nonnull TextRange scope, int fileLength, @Nonnull Object reason) {
    assertAllowModifications();
    log("Mark scope dirty: ", scope, reason);
    synchronized (myDocumentToStatusMap) {
      FileStatus status = myDocumentToStatusMap.get(document);
      if (status == null) return; // all dirty already
      if (status.defensivelyMarked) {
        status.defensivelyMarked = false;
      }
      status.combineScopesWith(scope, fileLength, document);
    }
  }

  @Nonnull
  private static RangeMarker combineScopes(RangeMarker old, @Nonnull TextRange scope, int textLength, @Nonnull Document document) {
    if (old == null) {
      if (scope.equalsToRange(0, textLength)) return WHOLE_FILE_DIRTY_MARKER;
      return document.createRangeMarker(scope);
    }
    if (old == WHOLE_FILE_DIRTY_MARKER) return old;
    TextRange oldRange = TextRange.create(old);
    TextRange union = scope.union(oldRange);
    if (old.isValid() && union.equals(oldRange)) {
      return old;
    }
    if (union.getEndOffset() > textLength) {
      union = union.intersection(new TextRange(0, textLength));
    }
    assert union != null;
    return document.createRangeMarker(union);
  }

  boolean allDirtyScopesAreNull(@Nonnull Document document) {
    synchronized (myDocumentToStatusMap) {
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
      if (!ProblemHighlightFilter.shouldHighlightFile(file)) return true;

      FileStatus status = myDocumentToStatusMap.get(document);
      return status != null && !status.defensivelyMarked && status.wolfPassFinished && status.allDirtyScopesAreNull();
    }
  }

  @TestOnly
  public void assertAllDirtyScopesAreNull(@Nonnull Document document) {
    synchronized (myDocumentToStatusMap) {
      FileStatus status = myDocumentToStatusMap.get(document);
      assert status != null && !status.defensivelyMarked && status.wolfPassFinished && status.allDirtyScopesAreNull() : status;
    }
  }

  @TestOnly
  void allowDirt(boolean allow) {
    myAllowDirt = allow;
  }

  private static final RangeMarker WHOLE_FILE_DIRTY_MARKER = new RangeMarker() {
    @Nonnull
    @Override
    public Document getDocument() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getStartOffset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getEndOffset() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public void setGreedyToLeft(boolean greedy) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setGreedyToRight(boolean greedy) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGreedyToRight() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGreedyToLeft() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {
      // ignore
    }

    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
      return null;
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "WHOLE_FILE";
    }
  };

  // logging
  private static final ConcurrentMap<Thread, Integer> threads = ContainerUtil.createConcurrentWeakMap();

  private static int getThreadNum() {
    return ConcurrencyUtil.cacheOrGet(threads, Thread.currentThread(), threads.size());
  }

  public static void log(@Nonnull Object... info) {
    if (LOG.isDebugEnabled()) {
      String s = StringUtil.repeatSymbol(' ', getThreadNum() * 4) + Arrays.asList(info) + "\n";
      LOG.debug(s);
    }
  }
}
