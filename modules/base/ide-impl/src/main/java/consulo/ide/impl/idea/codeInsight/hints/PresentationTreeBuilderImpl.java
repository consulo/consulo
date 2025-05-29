// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ApplicationManager;
import consulo.language.editor.inlay.*;

import java.util.function.Consumer;

public class PresentationTreeBuilderImpl implements CollapsiblePresentationTreeBuilder {
    private final byte index;
    private final InlayTreeBuildingContext context;

    private PresentationTreeBuilderImpl(byte index, InlayTreeBuildingContext context) {
        this.index = index;
        this.context = context;
    }

    /**
     * Creates the root builder. Position may be null in unit tests only.
     */
    public static PresentationTreeBuilderImpl createRoot(InlayPosition position) {
        if (position == null && !ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalArgumentException("Position must not be null in production");
        }
        InlayPosition pos = position != null
            ? position
            : new InlayPosition.InlineInlayPosition(0, false, 0);
        InlayTreeBuildingContext context = new InlayTreeBuildingContext(pos);
        return new PresentationTreeBuilderImpl((byte) 0, context);
    }

    public static final int MAX_NODE_COUNT = 100;
    public static final int MAX_SEGMENT_TEXT_LENGTH = 30;
    public static final byte DOESNT_FIT_INDEX = -10;

    @Override
    public void list(Consumer<PresentationTreeBuilder> builder) {
        byte listIndex = context.addNode(index, InlayTags.LIST_TAG, null);
        builder.accept(new PresentationTreeBuilderImpl(listIndex, context));
    }

    @Override
    public void toggleButton(Consumer<PresentationTreeBuilder> builder) {
        byte buttonIndex = context.addNode(index, InlayTags.COLLAPSE_BUTTON_TAG, null);
        builder.accept(new PresentationTreeBuilderImpl(buttonIndex, context));
    }

    @Override
    public void collapsibleList(CollapseState state,
                                Consumer<CollapsiblePresentationTreeBuilder> expandedState,
                                Consumer<CollapsiblePresentationTreeBuilder> collapsedState) {
        byte tag;
        switch (state) {
            case Expanded:
                tag = InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG;
                break;
            case Collapsed:
                tag = InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG;
                break;
            default:
                tag = context.depth < 1
                    ? InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG
                    : InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG;
        }
        final byte listIndex = context.addNode(index, tag, null);

        // helper to add one branch
        Consumer<Branch> addChild = branch -> {
            context.depth++;
            try {
                byte childIndex = context.addNode(listIndex, branch.payload, null);
                branch.builder.accept(new PresentationTreeBuilderImpl(childIndex, context));
            }
            finally {
                context.depth--;
            }
        };

        if (tag == InlayTags.COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG) {
            addChild.accept(new Branch(InlayTags.COLLAPSIBLE_LIST_EXPANDED_BRANCH_TAG, expandedState));
            addChild.accept(new Branch(InlayTags.COLLAPSIBLE_LIST_COLLAPSED_BRANCH_TAG, collapsedState));
        }
        else {
            addChild.accept(new Branch(InlayTags.COLLAPSIBLE_LIST_COLLAPSED_BRANCH_TAG, collapsedState));
            addChild.accept(new Branch(InlayTags.COLLAPSIBLE_LIST_EXPANDED_BRANCH_TAG, expandedState));
        }
    }

    @Override
    public void text(String text, InlayActionData actionData) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text entry may not be empty. Please fix the provider implementation.");
        }
        String segmentText;
        if (context.isTruncateTextNodes() && text.length() > MAX_SEGMENT_TEXT_LENGTH) {
            segmentText = text.substring(0, MAX_SEGMENT_TEXT_LENGTH) + "…";
        }
        else {
            segmentText = text;
        }
        context.incrementTextElementCount();
        Object payload = (actionData != null)
            ? new ActionWithContent(actionData, segmentText)
            : segmentText;
        context.addNode(index, InlayTags.TEXT_TAG, payload);
    }

    @Override
    public void clickHandlerScope(InlayActionData actionData, Consumer<PresentationTreeBuilder> builder) {
        byte nodeIndex = context.addNode(index, InlayTags.CLICK_HANDLER_SCOPE_TAG, actionData);
        builder.accept(new PresentationTreeBuilderImpl(nodeIndex, context));
    }

    /**
     * Completes the build and returns the inlay tree.
     */
    public TinyTree<Object> complete() {
        TinyTree<Object> tree = context.getTree();
        tree.reverseChildren();
        if (context.getTextElementCount() == 0) {
            throw new IllegalStateException("No text nodes in presentation tree");
        }
        return tree;
    }

    private static class Branch {
        final byte payload;
        final Consumer<CollapsiblePresentationTreeBuilder> builder;

        Branch(byte payload, Consumer<CollapsiblePresentationTreeBuilder> builder) {
            this.payload = payload;
            this.builder = builder;
        }
    }
}
