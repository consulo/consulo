/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.internal.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class IndentAdjuster {
    private final AlignmentHelper myAlignmentHelper;
    private final BlockIndentOptions myBlockIndentOptions;

    public IndentAdjuster(BlockIndentOptions blockIndentOptions, AlignmentHelper alignmentHelper) {
        myAlignmentHelper = alignmentHelper;
        myBlockIndentOptions = blockIndentOptions;
    }

    /**
     * Sometimes to align block we adjust whitespace of other block before.
     * In such a case, we rollback to that block restarting formatting from there.
     *
     * @return new current block if we need to rollback, null otherwise
     */
    @RequiredReadAction
    LeafBlockWrapper adjustIndent(LeafBlockWrapper block) {
        AlignmentImpl alignment = CoreFormatterUtil.getAlignment(block);
        WhiteSpace whiteSpace = block.getWhiteSpace();

        if (alignment == null || myAlignmentHelper.shouldSkip(alignment)) {
            if (whiteSpace.containsLineFeeds()) {
                adjustSpacingByIndentOffset(block);
            }
            else {
                whiteSpace.arrangeSpaces(block.getSpaceProperty());
            }
            return null;
        }

        return myAlignmentHelper.applyAlignment(alignment, block);
    }

    private void adjustSpacingByIndentOffset(LeafBlockWrapper block) {
        CommonCodeStyleSettings.IndentOptions options = myBlockIndentOptions.getIndentOptions(block);
        IndentData offset = block.calculateOffset(options);
        block.getWhiteSpace().setSpaces(offset.getSpaces(), offset.getIndentSpaces());
    }

    public void adjustLineIndent(LeafBlockWrapper myCurrentBlock) {
        IndentData alignOffset = getAlignOffset(myCurrentBlock);

        if (alignOffset == null) {
            adjustSpacingByIndentOffset(myCurrentBlock);
        }
        else {
            myCurrentBlock.getWhiteSpace().setSpaces(alignOffset.getSpaces(), alignOffset.getIndentSpaces());
        }
    }

    @Nullable
    private static IndentData getAlignOffset(LeafBlockWrapper myCurrentBlock) {
        AbstractBlockWrapper current = myCurrentBlock;
        while (true) {
            AlignmentImpl alignment = current.getAlignment();
            LeafBlockWrapper offsetResponsibleBlock;
            if (alignment != null && (offsetResponsibleBlock = alignment.getOffsetRespBlockBefore(myCurrentBlock)) != null) {
                WhiteSpace whiteSpace = offsetResponsibleBlock.getWhiteSpace();
                if (whiteSpace.containsLineFeeds()) {
                    return new IndentData(whiteSpace.getIndentSpaces(), whiteSpace.getSpaces());
                }
                else {
                    int offsetBeforeBlock = CoreFormatterUtil.getStartColumn(offsetResponsibleBlock);
                    AbstractBlockWrapper indentedParentBlock = CoreFormatterUtil.getIndentedParentBlock(myCurrentBlock);
                    if (indentedParentBlock == null) {
                        return new IndentData(0, offsetBeforeBlock);
                    }
                    else {
                        int parentIndent = indentedParentBlock.getWhiteSpace().getIndentOffset();
                        if (parentIndent > offsetBeforeBlock) {
                            return new IndentData(0, offsetBeforeBlock);
                        }
                        else {
                            return new IndentData(parentIndent, offsetBeforeBlock - parentIndent);
                        }
                    }
                }
            }
            else {
                current = current.getParent();
                if (current == null || current.getStartOffset() != myCurrentBlock.getStartOffset()) {
                    return null;
                }
            }
        }
    }

    public IndentInfo adjustLineIndent(FormatProcessor.ChildAttributesInfo info) {
        AbstractBlockWrapper parent = info.parent;
        ChildAttributes childAttributes = info.attributes;
        int index = info.index;

        AlignWhiteSpace alignWhiteSpace = getAlignOffsetBefore(childAttributes.getAlignment());
        if (alignWhiteSpace == null) {
            return parent.calculateChildOffset(myBlockIndentOptions.getIndentOptions(parent), childAttributes, index).createIndentInfo();
        }
        else {
            return new IndentInfo(0, alignWhiteSpace.indentSpaces, alignWhiteSpace.alignSpaces);
        }
    }

    private static class AlignWhiteSpace {
        int indentSpaces = 0;
        int alignSpaces = 0;
    }

    private static AlignWhiteSpace getAlignOffsetBefore(@Nullable Alignment alignment) {
        if (alignment == null) {
            return null;
        }
        LeafBlockWrapper alignRespBlock = ((AlignmentImpl)alignment).getOffsetRespBlockBefore(null);
        return alignRespBlock != null ? getStartColumn(alignRespBlock) : null;
    }

    private static AlignWhiteSpace getStartColumn(@Nonnull LeafBlockWrapper block) {
        AlignWhiteSpace alignWhitespace = new AlignWhiteSpace();
        while (true) {
            WhiteSpace whiteSpace = block.getWhiteSpace();
            alignWhitespace.alignSpaces += whiteSpace.getSpaces();

            if (whiteSpace.containsLineFeeds()) {
                alignWhitespace.indentSpaces += whiteSpace.getIndentSpaces();
                return alignWhitespace;
            }
            else {
                alignWhitespace.alignSpaces += whiteSpace.getIndentSpaces();
            }

            block = block.getPreviousBlock();
            if (alignWhitespace.alignSpaces > CodeStyleConstraints.MAX_RIGHT_MARGIN || block == null) {
                return alignWhitespace;
            }
            alignWhitespace.alignSpaces += block.getSymbolsAtTheLastLine();
            if (block.containsLineFeeds()) {
                return alignWhitespace;
            }
        }
    }

}
