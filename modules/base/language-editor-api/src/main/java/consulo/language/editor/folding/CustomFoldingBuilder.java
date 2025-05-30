/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.util.collection.Stack;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds custom folding regions. If custom folding is supported for a language, its FoldingBuilder must be inherited from this class.
 *
 * @author Rustam Vishnyakov
 */
public abstract class CustomFoldingBuilder extends FoldingBuilderEx implements PossiblyDumbAware {
    private CustomFoldingProvider myDefaultProvider;
    private static final RegistryValue myMaxLookupDepth = Registry.get("custom.folding.max.lookup.depth");
    private static final ThreadLocal<Set<ASTNode>> ourCustomRegionElements = new ThreadLocal<>();

    @RequiredReadAction
    @Nonnull
    @Override
    public final FoldingDescriptor[] buildFoldRegions(@Nonnull PsiElement root, @Nonnull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        ourCustomRegionElements.set(new HashSet<>());
        try {
            if (CustomFoldingProvider.EP_NAME.hasAnyExtensions()) {
                myDefaultProvider = null;
                ASTNode rootNode = root.getNode();
                if (rootNode != null) {
                    addCustomFoldingRegionsRecursively(new FoldingStack(rootNode), rootNode, descriptors, 0);
                }
            }
            buildLanguageFoldRegions(descriptors, root, document, quick);
        }
        finally {
            ourCustomRegionElements.set(null);
        }
        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public final FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode node, @Nonnull Document document) {
        return buildFoldRegions(node.getPsi(), document, false);
    }

    /**
     * Implement this method to build language folding regions besides custom folding regions.
     *
     * @param descriptors The list of folding descriptors to store results to.
     * @param root        The root node for which the folding is requested.
     * @param document    The document for which folding is built.
     * @param quick       whether the result should be provided as soon as possible without reference resolving
     *                    and complex checks.
     */
    @RequiredReadAction
    protected abstract void buildLanguageFoldRegions(
        @Nonnull List<FoldingDescriptor> descriptors,
        @Nonnull PsiElement root,
        @Nonnull Document document,
        boolean quick
    );

    @RequiredReadAction
    private void addCustomFoldingRegionsRecursively(
        @Nonnull FoldingStack foldingStack,
        @Nonnull ASTNode node,
        @Nonnull List<FoldingDescriptor> descriptors,
        int currDepth
    ) {
        FoldingStack localFoldingStack = isCustomFoldingRoot(node) ? new FoldingStack(node) : foldingStack;
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (isCustomRegionStart(child)) {
                localFoldingStack.push(child);
            }
            else if (isCustomRegionEnd(child)) {
                if (!localFoldingStack.isEmpty()) {
                    ASTNode startNode = localFoldingStack.pop();
                    int startOffset = startNode.getTextRange().getStartOffset();
                    TextRange range = new TextRange(startOffset, child.getTextRange().getEndOffset());
                    descriptors.add(new FoldingDescriptor(startNode, range));
                    Set<ASTNode> nodeSet = ourCustomRegionElements.get();
                    nodeSet.add(startNode);
                    nodeSet.add(child);
                }
            }
            else {
                if (currDepth < myMaxLookupDepth.asInteger()) {
                    addCustomFoldingRegionsRecursively(localFoldingStack, child, descriptors, currDepth + 1);
                }
            }
        }
    }

    @RequiredReadAction
    @Override
    public final String getPlaceholderText(@Nonnull ASTNode node, @Nonnull TextRange range) {
        if (isCustomFoldingCandidate(node)) {
            String elementText = node.getText();
            CustomFoldingProvider defaultProvider = getDefaultProvider(elementText);
            if (defaultProvider != null && defaultProvider.isCustomRegionStart(elementText)) {
                return defaultProvider.getPlaceholderText(elementText);
            }
        }
        return getLanguagePlaceholderText(node, range);
    }

    protected abstract String getLanguagePlaceholderText(@Nonnull ASTNode node, @Nonnull TextRange range);


    @RequiredReadAction
    @Override
    public final String getPlaceholderText(@Nonnull ASTNode node) {
        return "...";
    }


    @RequiredReadAction
    @Override
    public final boolean isCollapsedByDefault(@Nonnull ASTNode node) {
        if (isCustomRegionStart(node)) {
            String childText = node.getText();
            CustomFoldingProvider defaultProvider = getDefaultProvider(childText);
            return defaultProvider != null && defaultProvider.isCollapsedByDefault(childText);
        }
        return isRegionCollapsedByDefault(node);
    }

    /**
     * Returns the default collapsed state for the folding region related to the specified node.
     *
     * @param node the node for which the collapsed state is requested.
     * @return true if the region is collapsed by default, false otherwise.
     */
    protected abstract boolean isRegionCollapsedByDefault(@Nonnull ASTNode node);

    /**
     * Returns true if the node corresponds to custom region start. The node must be a custom folding candidate and match custom folding
     * start pattern.
     *
     * @param node The node which may contain custom region start.
     * @return True if the node marks a custom region start.
     */
    public final boolean isCustomRegionStart(ASTNode node) {
        if (isCustomFoldingCandidate(node)) {
            String nodeText = node.getText();
            CustomFoldingProvider defaultProvider = getDefaultProvider(nodeText);
            return defaultProvider != null && defaultProvider.isCustomRegionStart(nodeText);
        }
        return false;
    }

    /**
     * Returns true if the node corresponds to custom region end. The node must be a custom folding candidate and match custom folding
     * end pattern.
     *
     * @param node The node which may contain custom region end
     * @return True if the node marks a custom region end.
     */
    protected final boolean isCustomRegionEnd(ASTNode node) {
        if (isCustomFoldingCandidate(node)) {
            String nodeText = node.getText();
            CustomFoldingProvider defaultProvider = getDefaultProvider(nodeText);
            return defaultProvider != null && defaultProvider.isCustomRegionEnd(nodeText);
        }
        return false;
    }

    protected static boolean isCustomRegionElement(PsiElement element) {
        Set<ASTNode> set = ourCustomRegionElements.get();
        return set != null && element != null && set.contains(element.getNode());
    }

    @Nullable
    private CustomFoldingProvider getDefaultProvider(String elementText) {
        if (myDefaultProvider == null) {
            for (CustomFoldingProvider provider : CustomFoldingProvider.EP_NAME.getExtensionList()) {
                if (provider.isCustomRegionStart(elementText) || provider.isCustomRegionEnd(elementText)) {
                    myDefaultProvider = provider;
                }
            }
        }
        return myDefaultProvider;
    }

    /**
     * Checks if a node may contain custom folding tags. By default returns true for PsiComment but a language folding builder may override
     * this method to allow only specific subtypes of comments (for example, line comments only).
     *
     * @param node The node to check.
     * @return True if the node may contain custom folding tags.
     */
    protected boolean isCustomFoldingCandidate(ASTNode node) {
        return node.getPsi() instanceof PsiComment;
    }

    public final boolean isCustomFoldingCandidate(@Nonnull PsiElement element) {
        ASTNode node = element.getNode();
        return node != null && isCustomFoldingCandidate(node);
    }

    /**
     * Checks if the node is used as custom folding root. Any custom folding elements inside the root are considered to be at the same level
     * even if they are located at different levels of PSI tree. By default the method returns true if the node has any child elements
     * (only custom folding comments at the same PSI tree level are processed, start/end comments at different levels will be ignored).
     *
     * @param node The node to check.
     * @return True if the node is a root for custom foldings.
     */
    protected boolean isCustomFoldingRoot(ASTNode node) {
        return node.getFirstChildNode() != null;
    }

    private static class FoldingStack extends Stack<ASTNode> {
        private final ASTNode owner;

        public FoldingStack(@Nonnull ASTNode owner) {
            super(1);
            this.owner = owner;
        }

        @Nonnull
        public ASTNode getOwner() {
            return owner;
        }
    }

    /**
     * Checks if the folding ranges can be created in the Dumb Mode. In the most of
     * language implementations the method returns true, but for strong context-dependent
     * languages (like ObjC/C++) overridden method returns false.
     *
     * @return True if the folding ranges can be created in the Dumb Mode
     */
    @Override
    public boolean isDumbAware() {
        return true;
    }
}
