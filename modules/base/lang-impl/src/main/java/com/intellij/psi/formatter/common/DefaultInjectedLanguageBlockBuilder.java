/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.formatter.common;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 9/6/12 9:22 AM
 */
public class DefaultInjectedLanguageBlockBuilder extends InjectedLanguageBlockBuilder {
  
  @Nonnull
  private final CodeStyleSettings mySettings;

  public DefaultInjectedLanguageBlockBuilder(@Nonnull CodeStyleSettings settings) {
    mySettings = settings;
  }

  @Nonnull
  @Override
  public CodeStyleSettings getSettings() {
    return mySettings;
  }

  @Override
  public boolean canProcessFragment(String text, ASTNode injectionHost) {
    return true;
  }

  @Override
  public Block createBlockBeforeInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, final TextRange range) {
    return new GlueBlock(node, wrap, alignment, indent, range);
  }

  @Override
  public Block createBlockAfterInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, TextRange range) {
    return new GlueBlock(node, wrap, alignment, Indent.getNoneIndent(), range);
  }

  private static class GlueBlock extends AbstractBlock {

    @Nonnull
    private final Indent    myIndent;
    @Nonnull
    private final TextRange myRange;

    private GlueBlock(@Nonnull ASTNode node,
                      @Nullable Wrap wrap,
                      @Nullable Alignment alignment,
                      @Nonnull Indent indent,
                      @Nonnull TextRange range)
    {
      super(node, wrap, alignment);
      myIndent = indent;
      myRange = range;
    }

    @Nonnull
    @Override
    public TextRange getTextRange() {
      return myRange;
    }

    @Override
    protected List<Block> buildChildren() {
      return AbstractBlock.EMPTY;
    }

    @Nonnull
    @Override
    public Indent getIndent() {
      return myIndent;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @Nonnull Block child2) {
      return null;
    }

    @Override
    public boolean isLeaf() {
      return true;
    }
  }
}
