/*
 * Copyright 2013-2018 consulo.io
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

package com.intellij.formatting.blocks;

import com.intellij.formatting.*;
import com.intellij.openapi.util.TextRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * from kotlin
 */
public class TextLineBlock implements Block {
  private final String myText;
  private final TextRange myTextRange;
  private final Alignment myAlignment;
  private final Indent myIndent;
  private final Spacing mySpacing;

  public TextLineBlock(String text, TextRange textRange, Alignment alignment, Indent indent, Spacing spacing) {
    myText = text;
    myTextRange = textRange;
    myAlignment = alignment;
    myIndent = indent;
    mySpacing = spacing;
  }

  public Spacing getSpacing() {
    return mySpacing;
  }

  public String getText() {
    return myText;
  }

  @Nonnull
  @Override
  public TextRange getTextRange() {
    return myTextRange;
  }

  @Nonnull
  @Override
  public List<Block> getSubBlocks() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public Wrap getWrap() {
    return null;
  }

  @Nullable
  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return myAlignment;
  }

  @Nullable
  @Override
  public Spacing getSpacing(@Nullable Block block, @Nonnull Block block1) {
    return mySpacing;
  }

  @Nonnull
  @Override
  public ChildAttributes getChildAttributes(int i) {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  @Override
  public String toString() {
    return "TextLineBlock(text='" + myText + "', textRange=" + myTextRange + ")";
  }
}
