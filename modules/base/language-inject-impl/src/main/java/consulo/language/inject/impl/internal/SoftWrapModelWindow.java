// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.codeEditor.*;
import consulo.codeEditor.event.SoftWrapChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;

class SoftWrapModelWindow implements SoftWrapModelEx {
  SoftWrapModelWindow() {
  }

  @Override
  public List<? extends SoftWrap> getRegisteredSoftWraps() {
    return Collections.emptyList();
  }

  @Override
  public int getSoftWrapIndex(int offset) {
    return -1;
  }

  @Override
  public int paint(@Nonnull Graphics g, @Nonnull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    return 0;
  }

  @Override
  public int getMinDrawingWidthInPixels(@Nonnull SoftWrapDrawingType drawingType) {
    return 0;
  }

  @Override
  public boolean addSoftWrapChangeListener(@Nonnull SoftWrapChangeListener listener) {
    return false;
  }

  @Override
  public boolean isRespectAdditionalColumns() {
    return false;
  }

  @Override
  public void forceAdditionalColumnsUsage() {
  }

  @Override
  public EditorTextRepresentationHelper getEditorTextRepresentationHelper() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSoftWrappingEnabled() {
    return false;
  }

  @Nullable
  @Override
  public SoftWrap getSoftWrap(int offset) {
    return null;
  }

  @Nonnull
  @Override
  public List<? extends SoftWrap> getSoftWrapsForRange(int start, int end) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public List<? extends SoftWrap> getSoftWrapsForLine(int documentLine) {
    return Collections.emptyList();
  }

  @Override
  public boolean isVisible(SoftWrap softWrap) {
    return false;
  }

  @Override
  public void beforeDocumentChangeAtCaret() {
  }

  @Override
  public boolean isInsideSoftWrap(@Nonnull VisualPosition position) {
    return false;
  }

  @Override
  public boolean isInsideOrBeforeSoftWrap(@Nonnull VisualPosition visual) {
    return false;
  }

  @Override
  public void release() {
  }
}
