// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.codeStyle.inject;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DefaultInjectedLanguageBlockBuilder extends InjectedLanguageBlockBuilder {

    private final CodeStyleSettings mySettings;

    public DefaultInjectedLanguageBlockBuilder(CodeStyleSettings settings) {
        mySettings = settings;
    }

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

    private static final class GlueBlock extends AbstractBlock {

        private final Indent myIndent;
        private final TextRange myRange;

        private GlueBlock(ASTNode node,
                          @Nullable Wrap wrap,
                          @Nullable Alignment alignment,
                          Indent indent,
                          TextRange range) {
            super(node, wrap, alignment);
            myIndent = indent;
            myRange = range;
        }

        @Override
        public TextRange getTextRange() {
            return myRange;
        }

        @Override
        protected List<Block> buildChildren() {
            return AbstractBlock.EMPTY;
        }

        @Override
        public Indent getIndent() {
            return myIndent;
        }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, Block child2) {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }
    }
}
