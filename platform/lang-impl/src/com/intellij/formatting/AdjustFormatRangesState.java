/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.formatting;

import com.intellij.formatting.engine.State;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.formatter.common.ExtraRangesProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-May-17
 * <p>
 * from kotlin platform\lang-impl\src\com\intellij\formatting\AdjustFormatRangesState.kt
 */
public class AdjustFormatRangesState extends State {
  private FormatTextRanges formatRanges;

  private final Stack<Block> state;
  private List<TextRange> totalNewRanges = new ArrayList<>();
  private List<TextRange> extendedRanges;

  public AdjustFormatRangesState(Block currentBlock, FormatTextRanges formatRanges) {
    this.formatRanges = formatRanges;
    state = new Stack<>(currentBlock);

    extendedRanges = formatRanges.getExtendedFormattingRanges();

    setOnDone(() -> totalNewRanges.forEach(it -> this.formatRanges.add(it, false)));
  }

  @Override
  protected void doIteration() {
    Block currentBlock = state.pop();
    processBlock(currentBlock);
    setDone(state.isEmpty());
  }

  private void processBlock(Block currentBlock) {
    if (!isInsideExtendedFormattingRanges(currentBlock)) return;

    ContainerUtil.reverse(currentBlock.getSubBlocks()).forEach(state::push);

    if (!formatRanges.isReadOnly(currentBlock.getTextRange())) {
      extractRanges(currentBlock);
    }
  }

  private boolean isInsideExtendedFormattingRanges(Block currentBlock) {
    return ContainerUtil.find(extendedRanges, it -> it.intersects(currentBlock.getTextRange())) != null;
  }

  private void extractRanges(Block block) {
    if (block instanceof ExtraRangesProvider) {
      List<TextRange> newRanges = ((ExtraRangesProvider)block).getExtraRangesToFormat(formatRanges);
      if (newRanges != null) {
        totalNewRanges.addAll(newRanges);
      }
    }
  }
}
