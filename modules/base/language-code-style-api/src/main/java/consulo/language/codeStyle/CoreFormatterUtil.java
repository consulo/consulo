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
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.internal.*;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains utility methods for core formatter processing.
 */
public class CoreFormatterUtil {
    private CoreFormatterUtil() {
    }

    @Nonnull
    public static FormattingModel buildModel(
        @Nonnull FormattingModelBuilder builder,
        @Nonnull PsiElement element,
        @Nonnull TextRange range,
        @Nonnull CodeStyleSettings settings,
        @Nonnull FormattingMode mode
    ) {
        return builder.createModel(FormattingContext.create(element, range, settings, mode));
    }

    @Nonnull
    @RequiredReadAction
    public static FormattingModel buildModel(
        @Nonnull FormattingModelBuilder builder,
        @Nonnull PsiElement element,
        @Nonnull CodeStyleSettings settings,
        @Nonnull FormattingMode mode
    ) {
        return buildModel(builder, element, element.getTextRange(), settings, mode);
    }

    /**
     * Checks if there is an {@link AlignmentImpl} object that should be used during adjusting
     * {@link AbstractBlockWrapper#getWhiteSpace() white space} of the given block.
     *
     * @param block target block
     * @return alignment object to use during adjusting white space of the given block if any; {@code null} otherwise
     */
    @Nullable
    public static AlignmentImpl getAlignment(@Nonnull AbstractBlockWrapper block) {
        AbstractBlockWrapper current = block;
        while (true) {
            AlignmentImpl alignment = current.getAlignment();
            if (alignment == null || alignment.getOffsetRespBlockBefore(block) == null) {
                current = current.getParent();
                if (current == null || current.getStartOffset() != block.getStartOffset()) {
                    return null;
                }
            }
            else {
                return alignment;
            }
        }
    }

    /**
     * Calculates number of non-line feed symbols before the given wrapped block.
     * <p/>
     * <b>Example:</b>
     * <pre>
     *      whitespace<sub>11</sub> block<sub>11</sub> whitespace<sub>12</sub> block<sub>12</sub>
     *      whitespace<sub>21</sub> block<sub>21</sub> whitespace<sub>22</sub> block<sub>22</sub>
     * </pre>
     * <p/>
     * Suppose this method is called with the wrapped <code>'block<sub>22</sub>'</code> and <code>'whitespace<sub>21</sub>'</code>
     * contains line feeds but <code>'whitespace<sub>22</sub>'</code> is not. This method returns number of symbols
     * from <code>'whitespace<sub>21</sub>'</code> after its last line feed symbol plus number of symbols at
     * <code>block<sub>21</sub></code> plus number of symbols at <code>whitespace<sub>22</sub></code>.
     *
     * @param block target wrapped block to be used at a boundary during counting non-line feed symbols to the left of it
     * @return non-line feed symbols to the left of the given wrapped block
     */
    public static int getStartColumn(@Nullable LeafBlockWrapper block) {
        if (block == null) {
            return -1;
        }

        int result = 0;
        while (true) {
            WhiteSpace whiteSpace = block.getWhiteSpace();
            result += whiteSpace.getTotalSpaces();
            if (whiteSpace.containsLineFeeds()) {
                return result;
            }
            block = block.getPreviousBlock();
            if (result > CodeStyleConstraints.MAX_RIGHT_MARGIN || block == null) {
                return result;
            }
            result += block.getSymbolsAtTheLastLine();
            if (block.containsLineFeeds()) {
                return result;
            }
        }
    }

    /**
     * Tries to find the closest block that starts before the given block and contains line feeds.
     *
     * @return closest block to the given block that contains line feeds if any; {@code null} otherwise
     */
    @Nullable
    public static AbstractBlockWrapper getIndentedParentBlock(@Nonnull AbstractBlockWrapper block) {
        AbstractBlockWrapper current = block.getParent();
        while (current != null) {
            if (current.getStartOffset() != block.getStartOffset() && current.getWhiteSpace().containsLineFeeds()) {
                return current;
            }
            if (current.getParent() != null) {
                AbstractBlockWrapper prevIndented = current.getParent().getPrevIndentedSibling(current);
                if (prevIndented != null) {
                    return prevIndented;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * It's possible to configure alignment in a way to allow
     * {@link AlignmentFactory#createAlignment(boolean, Alignment.Anchor)}  backward shift}.
     * <p/>
     * <b>Example:</b>
     * <pre>
     *     class Test {
     *         int i;
     *         StringBuilder buffer;
     *     }
     * </pre>
     * <p/>
     * It's possible that blocks {@code 'i'} and {@code 'buffer'} should be aligned. As formatter processes document from
     * start to end that means that requirement to shift block {@code 'i'} to the right is discovered only during
     * {@code 'buffer'} block processing. I.e. formatter returns to the previously processed block ({@code 'i'}), modifies
     * its white space and continues from that location (performs 'backward' shift).
     * <p/>
     * Here is one very important moment - there is a possible case that formatting blocks are configured in a way that they are
     * combined in explicit cyclic graph.
     * <p/>
     * <b>Example:</b>
     * <pre>
     *     blah(bleh(blih,
     *       bloh), bluh);
     * </pre>
     * <p/>
     * Consider that pairs of blocks {@code 'blih'; 'bloh'} and {@code 'bleh', 'bluh'} should be aligned
     * and backward shift is possible for them. Here is how formatter works:
     * <ol>
     * <li>
     * Processing reaches <b>'bloh'</b> block. It's aligned to {@code 'blih'} block. Current document state:
     * <p/>
     * <pre>
     *          blah(bleh(blih,
     *                    bloh), bluh);
     *      </pre>
     * </li>
     * <li>
     * Processing reaches <b>'bluh'</b> block. It's aligned to {@code 'blih'} block and backward shift is allowed, hence,
     * {@code 'blih'} block is moved to the right and processing contnues from it. Current document state:
     * <pre>
     *          blah(            bleh(blih,
     *                    bloh), bluh);
     *      </pre>
     * </li>
     * <li>
     * Processing reaches <b>'bloh'</b> block. It's configured to be aligned to {@code 'blih'} block, hence, it's moved
     * to the right:
     * <pre>
     *          blah(            bleh(blih,
     *                                bloh), bluh);
     *      </pre>
     * </li>
     * <li>We have endless loop then;</li>
     * </ol>
     * So, that implies that we can't use backward alignment if the blocks are configured in a way that backward alignment
     * appliance produces endless loop. This method encapsulates the logic for checking if backward alignment can be applied.
     *
     * @param first             the first aligned block
     * @param second            the second aligned block
     * @param alignmentMappings block aligned mappings info
     * @return {@code true} if backward alignment is possible; {@code false} otherwise
     */
    public static boolean allowBackwardAlignment(
        @Nonnull LeafBlockWrapper first,
        @Nonnull LeafBlockWrapper second,
        @Nonnull Map<AbstractBlockWrapper, Set<AbstractBlockWrapper>> alignmentMappings
    ) {
        Set<AbstractBlockWrapper> blocksBeforeCurrent = new HashSet<>();
        for (LeafBlockWrapper previousBlock = second.getPreviousBlock(); previousBlock != null;
             previousBlock = previousBlock.getPreviousBlock()) {
            Set<AbstractBlockWrapper> blocks = alignmentMappings.get(previousBlock);
            if (blocks != null) {
                blocksBeforeCurrent.addAll(blocks);
            }

            if (previousBlock.getWhiteSpace().containsLineFeeds()) {
                break;
            }
        }

        for (LeafBlockWrapper next = first.getNextBlock();
             next != null && !next.getWhiteSpace().containsLineFeeds();
             next = next.getNextBlock()) {
            if (blocksBeforeCurrent.contains(next)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates indent for the given block and target start offset according to the given indent options.
     *
     * @param options               indent options to use
     * @param block                 target wrapped block
     * @param tokenBlockStartOffset target wrapped block offset
     * @return indent to use for the given parameters
     */
    public static IndentData getIndent(
        CommonCodeStyleSettings.IndentOptions options,
        AbstractBlockWrapper block,
        int tokenBlockStartOffset
    ) {
        IndentImpl indent = block.getIndent();
        if (indent.getType() == Indent.Type.CONTINUATION) {
            return new IndentData(options.CONTINUATION_INDENT_SIZE);
        }
        if (indent.getType() == Indent.Type.CONTINUATION_WITHOUT_FIRST) {
            if (block.getStartOffset() != block.getParent().getStartOffset() && block.getStartOffset() == tokenBlockStartOffset) {
                return new IndentData(options.CONTINUATION_INDENT_SIZE);
            }
            else {
                return new IndentData(0);
            }
        }
        if (indent.getType() == Indent.Type.LABEL) {
            return new IndentData(options.LABEL_INDENT_SIZE);
        }
        if (indent.getType() == Indent.Type.NONE) {
            return new IndentData(0);
        }
        if (indent.getType() == Indent.Type.SPACES) {
            return new IndentData(indent.getSpaces(), 0);
        }
        return new IndentData(options.INDENT_SIZE);
    }

    @Nonnull
    public static LeafBlockWrapper getFirstLeaf(@Nonnull AbstractBlockWrapper block) {
        if (block instanceof LeafBlockWrapper leafBlockWrapper) {
            return leafBlockWrapper;
        }
        else {
            return getFirstLeaf(((CompositeBlockWrapper)block).getChildren().get(0));
        }
    }
}
