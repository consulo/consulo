// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class LookupOffsets implements DocumentListener {
  @Nonnull
  private String myAdditionalPrefix = "";

  private boolean myStableStart;
  @Nullable
  private Supplier<String> myStartMarkerDisposeInfo = null;
  @Nonnull
  private RangeMarker myLookupStartMarker;
  private int myRemovedPrefix;
  private final RangeMarker myLookupOriginalStartMarker;
  private final Editor myEditor;

  public LookupOffsets(Editor editor) {
    myEditor = editor;
    int caret = getPivotOffset();
    myLookupOriginalStartMarker = createLeftGreedyMarker(caret);
    myLookupStartMarker = createLeftGreedyMarker(caret);
    myEditor.getDocument().addDocumentListener(this);
  }

  @Override
  public void documentChanged(@Nonnull DocumentEvent e) {
    if (myStartMarkerDisposeInfo == null && !myLookupStartMarker.isValid()) {
      Throwable throwable = new Throwable();
      String eString = e.toString();
      myStartMarkerDisposeInfo = () -> eString + "\n" + ExceptionUtil.getThrowableText(throwable);
    }
  }

  private RangeMarker createLeftGreedyMarker(int start) {
    RangeMarker marker = myEditor.getDocument().createRangeMarker(start, start);
    marker.setGreedyToLeft(true);
    return marker;
  }

  private int getPivotOffset() {
    return myEditor.getSelectionModel().hasSelection() ? myEditor.getSelectionModel().getSelectionStart() : myEditor.getCaretModel()
                                                                                                                    .getOffset();
  }

  @Nonnull
  public String getAdditionalPrefix() {
    return myAdditionalPrefix;
  }

  public void appendPrefix(char c) {
    myAdditionalPrefix += c;
  }

  public boolean truncatePrefix() {
    final int len = myAdditionalPrefix.length();
    if (len == 0) {
      myRemovedPrefix++;
      return false;
    }
    myAdditionalPrefix = myAdditionalPrefix.substring(0, len - 1);
    return true;
  }

  public void destabilizeLookupStart() {
    myStableStart = false;
  }

  public void checkMinPrefixLengthChanges(Collection<? extends LookupElement> items, LookupEx lookup) {
    if (myStableStart) return;
    if (!lookup.isCalculating() && !items.isEmpty()) {
      myStableStart = true;
    }

    int minPrefixLength = items.isEmpty() ? 0 : Integer.MAX_VALUE;
    for (final LookupElement item : items) {
      if (!(item instanceof EmptyLookupItem)) {
        minPrefixLength = Math.min(lookup.itemMatcher(item).getPrefix().length(), minPrefixLength);
      }
    }

    int start = getPivotOffset() - minPrefixLength - myAdditionalPrefix.length() + myRemovedPrefix;
    start = Math.max(Math.min(start, myEditor.getDocument().getTextLength()), 0);
    if (myLookupStartMarker.isValid() && myLookupStartMarker.getStartOffset() == start && myLookupStartMarker.getEndOffset() == start) {
      return;
    }

    myLookupStartMarker.dispose();
    myLookupStartMarker = createLeftGreedyMarker(start);
    myStartMarkerDisposeInfo = null;
  }

  public int getLookupStart(@Nullable Throwable disposeTrace) {
    if (!myLookupStartMarker.isValid()) {
      throw new AssertionError("Invalid lookup start: " +
                                 myLookupStartMarker +
                                 ", " +
                                 myEditor +
                                 ", disposeTrace=" +
                                 (disposeTrace == null ? null : ExceptionUtil.getThrowableText(disposeTrace)) +
                                 "\n================\n start dispose trace=" +
                                 (myStartMarkerDisposeInfo == null ? null : myStartMarkerDisposeInfo.get()));
    }
    return myLookupStartMarker.getStartOffset();
  }

  public int getLookupOriginalStart() {
    return myLookupOriginalStartMarker.isValid() ? myLookupOriginalStartMarker.getStartOffset() : -1;
  }

  public boolean performGuardedChange(Runnable change) {
    if (!myLookupStartMarker.isValid()) {
      throw new AssertionError("Invalid start: " + myEditor + ", trace=" + (myStartMarkerDisposeInfo == null ? null : myStartMarkerDisposeInfo
        .get()));
    }
    change.run();
    return myLookupStartMarker.isValid();
  }

  public void clearAdditionalPrefix() {
    myAdditionalPrefix = "";
    myRemovedPrefix = 0;
  }

  public void disposeMarkers() {
    myEditor.getDocument().removeDocumentListener(this);
    myLookupStartMarker.dispose();
    myLookupOriginalStartMarker.dispose();
  }

  public int getPrefixLength(LookupElement item, LookupEx lookup) {
    return lookup.itemPattern(item).length() - myRemovedPrefix;
  }
}
