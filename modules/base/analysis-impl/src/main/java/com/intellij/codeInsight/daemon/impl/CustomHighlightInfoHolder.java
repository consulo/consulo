// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesScheme;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class CustomHighlightInfoHolder extends HighlightInfoHolder {
  private final EditorColorsScheme myCustomColorsScheme;

  CustomHighlightInfoHolder(@Nonnull PsiFile contextFile, @Nullable EditorColorsScheme customColorsScheme, @Nonnull HighlightInfoFilter... filters) {
    super(contextFile, filters);
    myCustomColorsScheme = customColorsScheme;
  }

  @Override
  @Nonnull
  public TextAttributesScheme getColorsScheme() {
    if (myCustomColorsScheme != null) {
      return myCustomColorsScheme;
    }
    return EditorColorsManager.getInstance().getGlobalScheme();
  }
}
