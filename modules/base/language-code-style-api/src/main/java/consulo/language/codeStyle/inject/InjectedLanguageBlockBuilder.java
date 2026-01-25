// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.codeStyle.inject;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class InjectedLanguageBlockBuilder {
    private static final Logger LOG = Logger.getInstance(InjectedLanguageBlockBuilder.class);

    public @Nonnull Block createInjectedBlock(@Nonnull ASTNode node,
                                              @Nonnull Block originalBlock,
                                              Indent indent,
                                              int offset,
                                              TextRange range,
                                              @Nonnull Language language) {
        return new InjectedLanguageBlockWrapper(originalBlock, offset, range, indent, language);
    }

    public abstract CodeStyleSettings getSettings();

    protected boolean supportsMultipleFragments() {
        return false;
    }

    public abstract boolean canProcessFragment(String text, ASTNode injectionHost);

    public abstract Block createBlockBeforeInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, TextRange range);

    public abstract Block createBlockAfterInjection(ASTNode node, Wrap wrap, Alignment alignment, Indent indent, TextRange range);

    public boolean addInjectedBlocks(List<? super Block> result, ASTNode injectionHost, Wrap wrap, Alignment alignment, Indent indent) {
        SimpleReference<Integer> lastInjectionEndOffset = new SimpleReference<>(0);

        PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = (injectedPsi, places) -> {
            if (places.isEmpty() || (places.size() != 1 && !supportsMultipleFragments())) {
                return;
            }
            PsiLanguageInjectionHost.Shred firstShred = places.get(0);
            PsiLanguageInjectionHost.Shred lastShred = places.get(places.size() - 1);
            PsiLanguageInjectionHost shredHost = firstShred.getHost();
            if (shredHost == null) {
                return;
            }

            for (PsiLanguageInjectionHost.Shred place : places) {
                if (place.getHost() != shredHost) return;
            }

            TextRange injectionRange = new TextRange(firstShred.getRangeInsideHost().getStartOffset(),
                lastShred.getRangeInsideHost().getEndOffset());
            ASTNode node = shredHost.getNode();
            if (node == null || !injectionHost.getTextRange().contains(injectionRange.shiftRight(node.getStartOffset()))) {
                return;
            }
            if (node != injectionHost) {
                int shift = 0;
                boolean canProcess = false;
                for (ASTNode n = injectionHost.getTreeParent(), prev = injectionHost; n != null; prev = n, n = n.getTreeParent()) {
                    shift += n.getStartOffset() - prev.getStartOffset();
                    if (n == node) {
                        injectionRange = injectionRange.shiftRight(shift);
                        canProcess = true;
                        break;
                    }
                }
                if (!canProcess) {
                    return;
                }
            }

            String childText;
            if (injectionHost.getTextLength() == injectionRange.getEndOffset() && injectionRange.getStartOffset() == 0 ||
                canProcessFragment((childText = injectionHost.getText()).substring(0, injectionRange.getStartOffset()), injectionHost) &&
                    canProcessFragment(childText.substring(injectionRange.getEndOffset()), injectionHost)) {

                // inject language block

                Language childLanguage = injectedPsi.getLanguage();
                FormattingModelBuilder builder = FormattingModelBuilder.forContext(childLanguage, injectionHost.getPsi());

                if (builder != null) {
                    int startOffset = injectionRange.getStartOffset();
                    int endOffset = injectionRange.getEndOffset();
                    TextRange range = injectionHost.getTextRange();
                    int prefixLength = firstShred.getPrefix().length();
                    int suffixLength = lastShred.getSuffix().length();

                    int childOffset = range.getStartOffset();
                    if (lastInjectionEndOffset.get() < startOffset) {
                        result.add(createBlock(injectionHost, wrap, alignment, indent, new TextRange(lastInjectionEndOffset.get(), startOffset)));
                    }

                    addInjectedLanguageBlocks(result, injectedPsi, indent, childOffset + startOffset,
                        new TextRange(prefixLength, injectedPsi.getTextLength() - suffixLength), places);

                    lastInjectionEndOffset.set(endOffset);
                }
            }
        };
        PsiElement injectionHostPsi = injectionHost.getPsi();
        PsiFile containingFile = injectionHostPsi.getContainingFile();
        InjectedLanguageManager.getInstance(containingFile.getProject())
            .enumerateEx(injectionHostPsi, containingFile, true, injectedPsiVisitor);

        if (lastInjectionEndOffset.get() > 0) {
            if (lastInjectionEndOffset.get() < injectionHost.getTextLength()) {
                result.add(createBlock(injectionHost, wrap, alignment, indent,
                    new TextRange(lastInjectionEndOffset.get(), injectionHost.getTextLength())));
            }
            return true;
        }
        return false;
    }

    private Block createBlock(ASTNode injectionHost, Wrap wrap, Alignment alignment, Indent indent, TextRange range) {
        if (range.getStartOffset() == 0) {
            ASTNode leaf = injectionHost.findLeafElementAt(range.getEndOffset() - 1);
            return createBlockBeforeInjection(
                leaf, wrap, alignment, indent, range.shiftRight(injectionHost.getStartOffset()));
        }
        ASTNode leaf = injectionHost.findLeafElementAt(range.getStartOffset());
        return createBlockAfterInjection(
            leaf, wrap, alignment, indent, range.shiftRight(injectionHost.getStartOffset()));
    }

    protected void addInjectedLanguageBlocks(List<? super Block> result,
                                             PsiFile injectedFile,
                                             Indent indent,
                                             int offset,
                                             TextRange injectedEditableRange,
                                             List<? extends PsiLanguageInjectionHost.Shred> shreds) {
        addInjectedLanguageBlockWrapper(result, injectedFile.getNode(), indent, offset, injectedEditableRange);
    }

    public void addInjectedLanguageBlockWrapper(List<? super Block> result, ASTNode injectedNode,
                                                Indent indent, int offset, @Nullable TextRange range) {
        if (isEmptyRange(injectedNode, range)) {
            return;
        }

        PsiElement childPsi = injectedNode.getPsi();
        Language childLanguage = childPsi.getLanguage();
        FormattingModelBuilder builder = FormattingModelBuilder.forContext(childLanguage, childPsi);
        LOG.assertTrue(builder != null);
        FormattingModel childModel = builder.createModel(FormattingContext.create(childPsi, getSettings()));
        if (!childPsi.getTextRange().contains(childModel.getRootBlock().getTextRange())) {
            LOG.error("Invalid formatter model created for injected language fragment. Node rage: " + childPsi.getTextRange() +
                "; created model range: " + childModel.getRootBlock().getTextRange() +
                "; builder: " + builder.getClass().getName() +
                "; injected language: " + childLanguage.getID() +
                "; file: " + childPsi.getContainingFile().getName());
            return;
        }
        Block original = childModel.getRootBlock();

        if (original.isLeaf() && !injectedNode.getText().trim().isEmpty() || !original.getSubBlocks().isEmpty()) {
            result.add(createInjectedBlock(injectedNode, original, indent, offset, range, childLanguage));
        }
    }

    protected static boolean isEmptyRange(@Nonnull ASTNode injectedNode, @Nullable TextRange range) {
        return range != null && (range.getLength() == 0 || StringUtil.isEmptyOrSpaces(range.substring(injectedNode.getText())));
    }
}