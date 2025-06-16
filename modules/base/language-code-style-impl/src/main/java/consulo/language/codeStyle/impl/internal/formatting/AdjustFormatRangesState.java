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
package consulo.language.codeStyle.impl.internal.formatting;

import consulo.document.util.TextRange;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.ExtraRangesProvider;
import consulo.language.codeStyle.FormatTextRanges;
import consulo.language.codeStyle.FormattingDocumentModel;
import consulo.language.codeStyle.internal.RangesAssert;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Stack;

import java.util.ArrayList;
import java.util.List;

class AdjustFormatRangesState extends State {
  private final static RangesAssert ASSERT = new RangesAssert();

  private final FormatTextRanges myFormatRanges;
  private final List<TextRange> myExtendedRanges;
  private final List<TextRange> totalNewRanges = new ArrayList<>();
  private final Stack<Block> state;
  private final FormattingDocumentModel myModel;

  AdjustFormatRangesState(Block currentRoot, FormatTextRanges formatRanges, FormattingDocumentModel model) {
    myModel = model;
    myFormatRanges = formatRanges;
    myExtendedRanges = formatRanges.getExtendedRanges();
    state = new Stack<>(currentRoot);
    setOnDone(() -> totalNewRanges.forEach(range -> myFormatRanges.add(range, false)));
  }

  @Override
  public void doIteration() {
    Block currentBlock = state.pop();
    processBlock(currentBlock);
    setDone(state.isEmpty());
  }

  private void processBlock(Block currentBlock) {
    if (!isInsideExtendedFormattingRanges(currentBlock)) return;

    ContainerUtil.reverse(currentBlock.getSubBlocks()).stream().filter(block -> ASSERT.checkChildRange(currentBlock.getTextRange(), block.getTextRange(), myModel)).forEach(state::push);

    if (!myFormatRanges.isReadOnly(currentBlock.getTextRange())) {
      extractRanges(currentBlock);
    }
  }

  private boolean isInsideExtendedFormattingRanges(Block currentBlock) {
    TextRange blockRange = currentBlock.getTextRange();
    return myExtendedRanges.stream().anyMatch(range -> range.intersects(blockRange));
  }

  private void extractRanges(Block block) {
    if (block instanceof ExtraRangesProvider) {
      List<TextRange> newRanges = ((ExtraRangesProvider)block).getExtraRangesToFormat(myFormatRanges);
      if (newRanges != null) {
        totalNewRanges.addAll(newRanges);
      }
    }
  }
}
