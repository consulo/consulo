/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.inject;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.internal.DependantSpacingImpl;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class InjectedLanguageBlockWrapper implements BlockEx {
  private final Block myOriginal;
  private final int myOffset;
  private final TextRange myRange;
  @Nullable
  private final Indent myIndent;
  @Nullable
  private final Language myLanguage;
  private List<Block> myBlocks;

  /**
   * <pre>
   *  main code     prefix    injected code        suffix
   *     |            |            |                 |
   *     |          xxx!!!!!!!!!!!!!!!!!!!!!!!!!!!!xxx
   * ..................!!!!!!!!!!!!!!!!!!!!!!!!!!!!..........
   *                   ^
   *                 offset
   * </pre>
   *
   * @param original block inside injected code
   * @param offset   start offset of injected code inside the main document
   * @param range    range of code inside injected document which is really placed in the main document
   * @param indent
   */
  public InjectedLanguageBlockWrapper(@Nonnull Block original, int offset, @Nullable TextRange range, @Nullable Indent indent) {
    this(original, offset, range, indent, null);
  }

  public InjectedLanguageBlockWrapper(@Nonnull Block original,
                                      int offset,
                                      @Nullable TextRange range,
                                      @Nullable Indent indent,
                                      @Nullable Language language) {
    myOriginal = original;
    myOffset = offset;
    myRange = range;
    myIndent = indent;
    myLanguage = language;
  }

  @Override
  public Indent getIndent() {
    return myIndent != null ? myIndent : myOriginal.getIndent();
  }

  @Override
  @Nullable
  public Alignment getAlignment() {
    return myOriginal.getAlignment();
  }

  @Override
  @Nonnull
  public TextRange getTextRange() {
    TextRange range = myOriginal.getTextRange();
    if (myRange != null) {
      range = range.intersection(myRange);
    }

    int start = myOffset + range.getStartOffset() - (myRange != null ? myRange.getStartOffset() : 0);
    return TextRange.from(start, range.getLength());
  }

  @Nullable
  @Override
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  @Nonnull
  public List<Block> getSubBlocks() {
    if (myBlocks == null) {
      myBlocks = buildBlocks();
    }
    return myBlocks;
  }

  private List<Block> buildBlocks() {
    List<Block> list = myOriginal.getSubBlocks();
    if (list.isEmpty()) return AbstractBlock.EMPTY;
    if (myOffset == 0 && myRange == null) return list;

    ArrayList<Block> result = new ArrayList<>(list.size());
    if (myRange == null) {
      for (Block block : list) {
        result.add(new InjectedLanguageBlockWrapper(block, myOffset, myRange, null, myLanguage));
      }
    }
    else {
      collectBlocksIntersectingRange(list, result, myRange);
    }
    return result;
  }

  private void collectBlocksIntersectingRange(List<Block> list, List<Block> result, @Nonnull TextRange range) {
    for (Block block : list) {
      TextRange textRange = block.getTextRange();
      if (block instanceof InjectedLanguageBlockWrapper && block.getTextRange().equals(range)) {
        continue;
      }
      if (range.contains(textRange)) {
        result.add(new InjectedLanguageBlockWrapper(block, myOffset, range, null, myLanguage));
      }
      else if (textRange.intersectsStrict(range)) {
        collectBlocksIntersectingRange(block.getSubBlocks(), result, range);
      }
    }
  }

  @Override
  @Nullable
  public Wrap getWrap() {
    return myOriginal.getWrap();
  }

  @Override
  @Nullable
  public Spacing getSpacing(Block child1, @Nonnull Block child2) {
    int shift = 0;
    Block child1ToUse = child1;
    Block child2ToUse = child2;
    if (child1 instanceof InjectedLanguageBlockWrapper) {
      child1ToUse = ((InjectedLanguageBlockWrapper)child1).myOriginal;
      shift = child1.getTextRange().getStartOffset() - child1ToUse.getTextRange().getStartOffset();
    }
    if (child2 instanceof InjectedLanguageBlockWrapper) child2ToUse = ((InjectedLanguageBlockWrapper)child2).myOriginal;
    Spacing spacing = myOriginal.getSpacing(child1ToUse, child2ToUse);
    if (spacing instanceof DependantSpacingImpl && shift != 0) {
      DependantSpacingImpl hostSpacing = (DependantSpacingImpl)spacing;
      int finalShift = shift;
      List<TextRange> shiftedRanges = ContainerUtil.map(hostSpacing.getDependentRegionRanges(), range -> range.shiftRight(finalShift));
      return new DependantSpacingImpl(hostSpacing.getMinSpaces(),
                                      hostSpacing.getMaxSpaces(),
                                      shiftedRanges,
                                      hostSpacing.shouldKeepLineFeeds(),
                                      hostSpacing.getKeepBlankLines(),
                                      DependentSpacingRule.DEFAULT);
    }
    return spacing;
  }

  @Override
  @Nonnull
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return myOriginal.getChildAttributes(newChildIndex);
  }

  @Override
  public boolean isIncomplete() {
    return myOriginal.isIncomplete();
  }

  @Override
  public boolean isLeaf() {
    return myOriginal.isLeaf();
  }

  @Override
  public String toString() {
    return myOriginal.toString();
  }

  @Override
  @Nullable
  public String getDebugName() {
    if (myOriginal != null) {
      String originalDebugName = myOriginal.getDebugName();
      if (originalDebugName == null) originalDebugName = myOriginal.getClass().getSimpleName();
      return "wrapped " + originalDebugName;
    }
    else {
      return null;
    }
  }

  public Block getOriginal() {
    return myOriginal;
  }
}