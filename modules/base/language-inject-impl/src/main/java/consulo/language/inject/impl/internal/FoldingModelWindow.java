// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.inject.impl.internal;

import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.event.FoldingListener;
import consulo.colorScheme.TextAttributes;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.language.editor.inject.EditorWindow;
import consulo.document.DocumentWindow;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
class FoldingModelWindow implements FoldingModelEx, ModificationTracker {
  private final FoldingModelEx myDelegate;
  private final DocumentWindow myDocumentWindow;
  private final EditorWindow myEditorWindow;

  FoldingModelWindow(@Nonnull FoldingModelEx delegate, @Nonnull DocumentWindow documentWindow, @Nonnull EditorWindow editorWindow) {
    myDelegate = delegate;
    myDocumentWindow = documentWindow;
    myEditorWindow = editorWindow;
  }

  @Override
  public void setFoldingEnabled(boolean isEnabled) {
    myDelegate.setFoldingEnabled(isEnabled);
  }

  @Override
  public boolean isFoldingEnabled() {
    return myDelegate.isFoldingEnabled();
  }

  @Override
  public FoldRegion getFoldingPlaceholderAt(@Nonnull Point p) {
    return myDelegate.getFoldingPlaceholderAt(p);
  }

  @Override
  public boolean intersectsRegion(int startOffset, int endOffset) {
    int hostStart = myDocumentWindow.injectedToHost(startOffset);
    int hostEnd = myDocumentWindow.injectedToHost(endOffset);
    return myDelegate.intersectsRegion(hostStart, hostEnd);
  }

  @Override
  public FoldRegion addFoldRegion(int startOffset, int endOffset, @Nonnull String placeholderText) {
    return createFoldRegion(startOffset, endOffset, placeholderText, null, false);
  }

  @Override
  public void removeFoldRegion(@Nonnull FoldRegion region) {
    myDelegate.removeFoldRegion((FoldRegion)((FoldingRegionWindow)region).getDelegate());
  }

  @Override
  @Nonnull
  public FoldRegion[] getAllFoldRegions() {
    FoldRegion[] all = myDelegate.getAllFoldRegions();
    List<FoldRegion> result = new ArrayList<>();
    for (FoldRegion region : all) {
      FoldingRegionWindow window = getWindowRegion(region);
      if (window != null) {
        result.add(window);
      }
    }
    return result.toArray(FoldRegion.EMPTY_ARRAY);
  }

  @Override
  public boolean isOffsetCollapsed(int offset) {
    return myDelegate.isOffsetCollapsed(myDocumentWindow.injectedToHost(offset));
  }

  @Override
  public FoldRegion getCollapsedRegionAtOffset(int offset) {
    FoldRegion host = myDelegate.getCollapsedRegionAtOffset(myDocumentWindow.injectedToHost(offset));
    return host; //todo convert to window?
  }

  @Nullable
  @Override
  public FoldRegion getFoldRegion(int startOffset, int endOffset) {
    TextRange range = new TextRange(startOffset, endOffset);
    TextRange hostRange = myDocumentWindow.injectedToHost(range);
    FoldRegion hostRegion = myDelegate.getFoldRegion(hostRange.getStartOffset(), hostRange.getEndOffset());
    return hostRegion == null ? null : getWindowRegion(hostRegion);
  }

  @Nullable
  private FoldingRegionWindow getWindowRegion(@Nonnull FoldRegion hostRegion) {
    FoldingRegionWindow window = hostRegion.getUserData(FOLD_REGION_WINDOW);
    return window != null && window.getEditor() == myEditorWindow ? window : null;
  }

  @Override
  public void runBatchFoldingOperation(@Nonnull Runnable operation) {
    myDelegate.runBatchFoldingOperation(operation);
  }

  @Override
  public void runBatchFoldingOperation(@Nonnull Runnable operation, boolean moveCaretFromCollapsedRegion) {
    myDelegate.runBatchFoldingOperation(operation, moveCaretFromCollapsedRegion);
  }

  @Override
  public void runBatchFoldingOperationDoNotCollapseCaret(@Nonnull Runnable operation) {
    myDelegate.runBatchFoldingOperationDoNotCollapseCaret(operation);
  }

  @Override
  public int getLastCollapsedRegionBefore(int offset) {
    return -1; //todo implement
  }

  @Override
  public TextAttributes getPlaceholderAttributes() {
    return myDelegate.getPlaceholderAttributes();
  }

  @Override
  public FoldRegion[] fetchTopLevel() {
    return FoldRegion.EMPTY_ARRAY; //todo implement
  }

  private static final Key<FoldingRegionWindow> FOLD_REGION_WINDOW = Key.create("FOLD_REGION_WINDOW");

  @Override
  public FoldRegion createFoldRegion(int startOffset, int endOffset, @Nonnull String placeholder, FoldingGroup group, boolean neverExpands) {
    TextRange hostRange = myDocumentWindow.injectedToHost(new TextRange(startOffset, endOffset));
    if (hostRange.getLength() < 2) return null;
    FoldRegion hostRegion = myDelegate.createFoldRegion(hostRange.getStartOffset(), hostRange.getEndOffset(), placeholder, group, neverExpands);
    if (hostRegion == null) return null;
    int startShift = Math.max(0, myDocumentWindow.hostToInjected(hostRange.getStartOffset()) - startOffset);
    int endShift = Math.max(0, endOffset - myDocumentWindow.hostToInjected(hostRange.getEndOffset()) - startShift);
    FoldingRegionWindow window = new FoldingRegionWindow(myDocumentWindow, myEditorWindow, hostRegion, startShift, endShift);
    hostRegion.putUserData(FOLD_REGION_WINDOW, window);
    return window;
  }

  @Override
  public void addListener(@Nonnull FoldingListener listener, @Nonnull Disposable parentDisposable) {
    myDelegate.addListener(listener, parentDisposable);
  }

  @Override
  public void rebuild() {
    myDelegate.rebuild();
  }

  @Nonnull
  @Override
  public List<FoldRegion> getGroupedRegions(FoldingGroup group) {
    List<FoldRegion> hostRegions = myDelegate.getGroupedRegions(group);
    List<FoldRegion> result = new ArrayList<>();
    for (FoldRegion hostRegion : hostRegions) {
      FoldingRegionWindow window = getWindowRegion(hostRegion);
      if (window != null) result.add(window);
    }
    return result;
  }

  @Override
  public void clearDocumentRangesModificationStatus() {
  }

  @Override
  public boolean hasDocumentRegionChangedFor(@Nonnull FoldRegion region) {
    return false;
  }

  @Override
  public void clearFoldRegions() {
    myDelegate.clearFoldRegions();
  }

  @Override
  public long getModificationCount() {
    return myDelegate instanceof ModificationTracker ? ((ModificationTracker)myDelegate).getModificationCount() : 0;
  }
}
