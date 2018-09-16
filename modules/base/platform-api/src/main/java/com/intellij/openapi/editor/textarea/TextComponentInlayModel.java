// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.textarea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.openapi.editor.VisualPosition;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class TextComponentInlayModel implements InlayModel {
  @Nullable
  @Override
  public Inlay addInlineElement(int offset, boolean relatesToPrecedingText, @Nonnull EditorCustomElementRenderer renderer) {
    return null;
  }

  @Nonnull
  @Override
  public List<Inlay> getInlineElementsInRange(int startOffset, int endOffset) {
    return Collections.emptyList();
  }

  @Override
  public boolean hasInlineElementAt(int offset) {
    return false;
  }

  @Nullable
  @Override
  public Inlay getInlineElementAt(@Nonnull VisualPosition visualPosition) {
    return null;
  }

  @Nullable
  @Override
  public Inlay getElementAt(@Nonnull Point point) {
    return null;
  }

  @Override
  public void addListener(@Nonnull Listener listener, @Nonnull Disposable disposable) {
  }
}
