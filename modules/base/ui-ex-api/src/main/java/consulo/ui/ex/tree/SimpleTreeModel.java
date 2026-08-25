/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.ex.tree;

import consulo.application.ReadAction;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleTreeModel<N extends SimpleNode> implements TreeModel<N> {
    private static final Logger LOG = Logger.getInstance(SimpleTreeModel.class);

    private final Supplier<? extends N> myRootSupplier;

    public SimpleTreeModel(N root) {
        myRootSupplier = () -> root;
    }

    public SimpleTreeModel(Supplier<? extends N> rootSupplier) {
        myRootSupplier = rootSupplier;
    }

    public @Nullable N getRootNode() {
        return myRootSupplier.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void buildChildren(Function<N, TreeNode<N>> nodeFactory, @Nullable N parentValue) {
        N parent = parentValue == null ? getRootNode() : parentValue;
        if (parent == null) {
            return;
        }

        SimpleNode[] children = ReadAction.computeNotNull(parent::getChildren);
        for (SimpleNode child : children) {
            N value = (N) child;
            TreeNode<N> node = nodeFactory.apply(value);

            ReadAction.compute(value::update);

            node.setLeaf(isLeaf(value));
            node.setRenderer(SimpleTreeModel::render);
        }
    }

    private static boolean isLeaf(SimpleNode node) {
        return switch (node.getLeafState()) {
            case ALWAYS -> true;
            case NEVER, ASYNC -> false;
            case DEFAULT -> ReadAction.compute(() -> node.getChildren().length == 0);
        };
    }

    @RequiredUIAccess
    private static void render(SimpleNode node, TextItemPresentation presentation) {
        node.update();

        renderPresentation(presentation, node, node);

        try {
            ReadAction.compute(() -> presentation.withIcon(iconOf(node)));
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    @Override
    public boolean onDoubleClick(Tree<N> tree, TreeNode<N> node, @Nullable InputDetails inputDetails) {
        return !node.getValue().handleDoubleClickOrEnter(DataManager.getInstance().getDataContext(tree), inputDetails);
    }

    /**
     * The icon of a node can live in two places - the {@link NodeDescriptor} field filled by {@code setIcon},
     * and the {@link PresentationData} filled by the template presentation or {@code update(PresentationData)}.
     * The awt renderer paints the presentation one, and the descriptor→presentation sync is one way, so a node
     * which only ever touched its presentation has an empty descriptor field.
     */
    static @Nullable Image iconOf(NodeDescriptor descriptor) {
        if (descriptor instanceof PresentableNodeDescriptor<?> presentable) {
            Image icon = presentable.getPresentation().getIcon();
            if (icon != null) {
                return icon;
            }
        }
        return descriptor.getIcon();
    }

    /**
     * The descriptor carries everything the awt renderer paints - coloured fragments, the file status
     * foreground and the file colour background - and flattening it to {@code toString()} lost all of it.
     * The forced colour wins only where a fragment has no colour of its own, so the grey location text stays
     * grey while the name takes the status colour. The background of the presentation is what the awt tree
     * paints the whole row with - see {@code ProjectViewTree.getFileColorFor} - so it goes to the item rather
     * than to the fragments, which paint theirs behind their own run of text.
     */
    static void renderPresentation(
        TextItemPresentation itemPresentation,
        PresentableNodeDescriptor<?> presentable,
        NodeDescriptor descriptor
    ) {
        PresentationData presentation = presentable.getPresentation();
        ColorValue forced = presentation.getForcedTextForeground();

        itemPresentation.withBackgroundColor(presentation.getBackground());

        List<PresentableNodeDescriptor.ColoredFragment> fragments = presentation.getColoredText();
        if (fragments.isEmpty()) {
            itemPresentation.append(LocalizeValue.ofNullable(descriptor.toString()), toTextAttribute(null, forced));
            return;
        }

        for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
            itemPresentation.append(fragment.getText(), toTextAttribute(fragment.getAttributes(), forced));
        }
    }

    static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes, @Nullable ColorValue forced) {
        int style = 0;
        ColorValue foreground = null;
        ColorValue background = null;

        if (attributes != null) {
            if ((attributes.getStyle() & SimpleTextAttributes.STYLE_BOLD) != 0) {
                style |= TextAttribute.STYLE_BOLD;
            }
            if ((attributes.getStyle() & SimpleTextAttributes.STYLE_ITALIC) != 0) {
                style |= TextAttribute.STYLE_ITALIC;
            }
            foreground = attributes.foreground();
            background = attributes.background();
        }

        if (foreground == null) {
            foreground = forced;
        }

        return new TextAttribute(style, foreground, background);
    }
}
