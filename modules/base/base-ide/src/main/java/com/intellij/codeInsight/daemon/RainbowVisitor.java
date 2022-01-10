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
package com.intellij.codeInsight.daemon;

import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import consulo.util.dataholder.UserDataHolderEx;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RainbowVisitor implements HighlightVisitor {
  private HighlightInfoHolder myHolder;
  private RainbowHighlighter myRainbowHighlighter;

  @Nonnull
  @Override
  public abstract HighlightVisitor clone();

  @Nonnull
  protected RainbowHighlighter getHighlighter() {
    return myRainbowHighlighter;
  }

  @Override
  public final boolean analyze(@Nonnull PsiFile file,
                               boolean updateWholeFile,
                               @Nonnull HighlightInfoHolder holder,
                               @Nonnull Runnable action) {
    myHolder = holder;
    myRainbowHighlighter = new RainbowHighlighter(myHolder.getColorsScheme());
    try {
      action.run();
    }
    finally {
      myHolder = null;
      myRainbowHighlighter = null;
    }
    return true;
  }

  protected void addInfo(@Nullable HighlightInfo highlightInfo) {
    myHolder.add(highlightInfo);
  }

  protected HighlightInfo getInfo(@Nonnull final PsiElement context,
                                  @Nonnull final PsiElement rainbowElement,
                                  @Nonnull final String name,
                                  @Nullable final TextAttributesKey colorKey) {
    int colorIndex = UsedColors.getOrAddColorIndex((UserDataHolderEx)context, name, getHighlighter().getColorsCount());
    return getHighlighter().getInfo(colorIndex, rainbowElement, colorKey);
  }
}
