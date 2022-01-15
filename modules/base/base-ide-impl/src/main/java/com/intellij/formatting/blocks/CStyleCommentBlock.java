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

import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * from kotlin
 */
public class CStyleCommentBlock extends AbstractBlock {
  private static class LineInfo {
    private String myText;
    private TextRange myTextRange;

    private LineInfo(String text, TextRange textRange) {
      myText = text;
      myTextRange = textRange;
    }
  }

  @Nullable
  private final Indent myIndent;

  private NotNullLazyValue<List<LineInfo>> lines = NotNullLazyValue.createValue(() -> {
    List<Pair<Integer, Character>> list = new ArrayList<>();
    CharSequence chars = getNode().getChars();
    for (int i = 0; i < chars.length(); i++) {
      char aChar = chars.charAt(i);
      list.add(Pair.create(i, aChar));
    }

    List<List<Pair<Integer, Character>>> split = split(list, pair -> pair.getSecond().equals('\n'));

    return ContainerUtil.mapNotNull(split, pairs -> {
      List<Pair<Integer, Character>> block = ContainerUtil.dropWhile(pairs, pair -> Character.isWhitespace(pair.getSecond()));
      if (block.isEmpty()) {
        return null;
      }

      String text = StringUtil.join(block, it -> String.valueOf(it.getSecond()), "").trim();

      int startOffset = getNode().getStartOffset() + ContainerUtil.getFirstItem(block).getFirst();
      TextRange range = new TextRange(startOffset, startOffset + text.length());
      return new LineInfo(text, range);
    });
  });


  private NotNullLazyValue<Boolean> isCommentFormattable = NotNullLazyValue.createValue(() -> {
    List<LineInfo> value = lines.getValue();

    for (int i = 0; i < value.size(); i++) {
      // drop(1);
      if (i == 0) {
        continue;
      }

      LineInfo lineInfo = value.get(i);
      if (!lineInfo.myText.startsWith("*")) {
        return false;
      }
    }
    return true;
  });

  private <T> List<List<T>> split(List<T> list, Predicate<T> predicate) {
    if (list.isEmpty()) {
      return Collections.emptyList();
    }

    List<List<T>> result = new ArrayList<>();

    List<T> current = new ArrayList<T>();
    for (T e : list) {
      if (predicate.test(e)) {
        result.add(new ArrayList<>(current));
        current.clear();
      }
      else {
        current.add(e);
      }
    }

    if (!current.isEmpty()) {
      result.add(current);
    }
    return result;
  }

  public CStyleCommentBlock(@Nonnull ASTNode node, @Nullable Indent indent) {
    super(node, null, null);
    myIndent = indent;
  }

  @Override
  @Nullable
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  protected List<Block> buildChildren() {
    if (!isCommentFormattable.getValue()) {
      return Collections.emptyList();
    }

    return ContainerUtil.<LineInfo, Block>map(lines.getValue(), t -> {
      String text = t.myText;

      Indent indent = null;
      if (!isCommentFormattable.getValue()) {
        indent = null;
      }
      else if (text.startsWith("/*")) {
        indent = Indent.getNoneIndent();
      }
      else {
        indent = Indent.getSpaceIndent(1);
      }

      return new TextLineBlock(text, t.myTextRange, null, indent, null);
    });
  }

  @Nullable
  @Override
  public Spacing getSpacing(@Nullable Block child1, @Nonnull Block child2) {
    boolean isLicenseComment = child1 == null && FormatterTreeUtil.prev(getNode()) == null;
    if (isLicenseComment) {
      return Spacing.getReadOnlySpacing();
    }

    return child2.getSpacing(null, this);
  }

  public Spacing getSpacing() {
    if (isCommentFormattable.getValue()) {
      return null;
    }
    else {
      return Spacing.getReadOnlySpacing();
    }
  }

  @Override
  public boolean isLeaf() {
    return !isCommentFormattable.getValue();
  }
}
