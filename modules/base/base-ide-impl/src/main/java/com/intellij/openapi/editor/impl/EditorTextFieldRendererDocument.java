// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.LineIterator;
import com.intellij.openapi.editor.ex.RangeMarkerEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Processor;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;

/**
 * Read-only document optimized for rendering of millions of EditorTextFields.
 * The only mutating method is setText() which is extremely cheap.
 */
public class EditorTextFieldRendererDocument extends UserDataHolderBase implements DocumentEx {
  private final RangeMarkerTree<RangeMarkerEx> myRangeMarkers = new RangeMarkerTree<RangeMarkerEx>(this) {
  };
  private char[] myChars = ArrayUtil.EMPTY_CHAR_ARRAY;
  private String myString = "";
  private LineSet myLineSet = LineSet.createLineSet(myString);

  @Override
  public void setModificationStamp(long modificationStamp) {
  }

  @Override
  public void replaceText(@Nonnull CharSequence chars, long newModificationStamp) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setText(@Nonnull CharSequence text) {
    String s = StringUtil.convertLineSeparators(text.toString());
    myChars = new char[s.length()];
    s.getChars(0, s.length(), myChars, 0);
    myString = new String(myChars);
    myLineSet = LineSet.createLineSet(myString);
  }

  @Nonnull
  @Override
  public LineIterator createLineIterator() {
    return myLineSet.createIterator();
  }

  @Override
  public boolean removeRangeMarker(@Nonnull RangeMarkerEx rangeMarker) {
    return myRangeMarkers.removeInterval(rangeMarker);
  }

  @Override
  public void registerRangeMarker(@Nonnull RangeMarkerEx rangeMarker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
    myRangeMarkers.addInterval(rangeMarker, start, end, greedyToLeft, greedyToRight, false, layer);
  }

  @Override
  public boolean processRangeMarkers(@Nonnull Processor<? super RangeMarker> processor) {
    return myRangeMarkers.processAll(processor);
  }

  @Override
  public boolean processRangeMarkersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeMarker> processor) {
    return myRangeMarkers.processOverlappingWith(start, end, processor);
  }

  @Nonnull
  @Override
  public CharSequence getImmutableCharSequence() {
    return myString;
  }

  @Nonnull
  @Override
  public char[] getChars() {
    return myChars;
  }

  @Override
  public int getLineCount() {
    return myLineSet.findLineIndex(myChars.length) + 1;
  }

  @Override
  public int getLineNumber(int offset) {
    return myLineSet.findLineIndex(offset);
  }

  @Override
  public int getLineStartOffset(int line) {
    return myChars.length == 0 ? 0 : myLineSet.getLineStart(line);
  }

  @Override
  public int getLineEndOffset(int line) {
    return myChars.length == 0 ? 0 : myLineSet.getLineEnd(line);
  }

  @Override
  public void insertString(int offset, @Nonnull CharSequence s) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void deleteString(int startOffset, int endOffset) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void replaceString(int startOffset, int endOffset, @Nonnull CharSequence s) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Nonnull
  @Override
  public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Nonnull
  @Override
  public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
