/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.FormattingModel;
import consulo.language.codeStyle.internal.AbstractBlockWrapper;
import consulo.language.codeStyle.internal.CompositeBlockWrapper;
import consulo.language.codeStyle.internal.WhiteSpace;
import consulo.language.codeStyle.internal.WrapImpl;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.codeStyle.CodeStyleSettings;
import jakarta.annotation.Nonnull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enumerates formatting processing states.
 * 
 * @author Denis Zhdanov
 * @since 2/10/11 2:50 PM
 */
public enum FormattingStateId {

  /**
   * Corresponds to {@link InitialInfoBuilder#buildFrom(Block, int, CompositeBlockWrapper, WrapImpl, Block, boolean)}.
   * <p/>
   * I.e. the first thing formatter does retrieval of all {@link Block code blocks} from target {@link FormattingModel model}
   * and wrapping them in order to be able to store information about modified white spaces. That processing may trigger
   * blocks construction because most of our formatting models do that lazily, i.e. they define the
   * {@link FormattingModel#getRootBlock() root block} and build its sub-blocks only on the {@link Block#getSubBlocks() first request}.
   */
  WRAPPING_BLOCKS(2),

  /**
   * This element corresponds to the state when formatter sequentially processes {@link AbstractBlockWrapper wrapped code blocks}
   * and modifies their {@link WhiteSpace white spaces} according to the current {@link CodeStyleSettings code style settings}.
   */
  PROCESSING_BLOCKS(1),

  /**
   * This element corresponds to formatting phase when all {@link AbstractBlockWrapper wrapped code blocks} are processed and it's
   * time to apply the changes to the underlying document.
   */
  APPLYING_CHANGES(10);
  
  private final String myDescription;
  private final double myWeight;

  FormattingStateId(double weight) {
    myWeight = weight;
    myDescription = CodeInsightBundle.message("progress.reformat.stage." + toString().replace('_', '.').toLowerCase());
  }

  /**
   * @return    human-readable textual description of the current state id
   */
  public String getDescription() {
    return myDescription;
  }

  /**
   * @return      <code>'weight'</code> of the current state. Basically, it's assumed that every processing iteration of the state
   *              with greater weight is executed longer that processing iteration of the state with the lower weight
   */
  public double getProgressWeight() {
    return myWeight;
  }

  /**
   * @return    collection of formatting states that are assumed to be executed prior to the current one
   */
  @Nonnull
  public Set<FormattingStateId> getPreviousStates() {
    Set<FormattingStateId> result = EnumSet.noneOf(FormattingStateId.class);
    for (FormattingStateId state : values()) {
      if (state == this) {
        break;
      }
      result.add(state);
    }
    return result;
  }
}
