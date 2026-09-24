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
package consulo.execution.impl.internal.service;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.service.ServiceViewItemState;
import consulo.execution.service.ServiceViewOptions;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The tree of a unified services view - the counterpart of the swing {@link ServiceViewTree}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedServiceViewTree {
    private final UIAccess myUIAccess;
    private final Tree<ServiceViewItem> myTree;

    @RequiredUIAccess
    UnifiedServiceViewTree(UIAccess uiAccess, UnifiedServiceViewTreeModel treeModel, Supplier<ServiceViewOptions> viewOptions, Disposable parent) {
        myUIAccess = uiAccess;
        treeModel.setRenderer(new UnifiedServiceViewTreeCellRenderer(viewOptions));
        // the services model is read only on its invoker, as the swing tree does - see ServiceViewTreeModel
        myTree = Tree.create(treeModel, treeModel.getInvoker());
        Disposer.register(parent, myTree.destroyHook());
    }

    Tree<ServiceViewItem> getComponent() {
        return myTree;
    }

    @Nullable ServiceViewItem getSelectedItem() {
        TreeNode<ServiceViewItem> node = myTree.getSelectedNode();
        return node == null ? null : node.getValue();
    }

    void addSelectionListener(Runnable listener) {
        myTree.addSelectListener(event -> listener.run());
    }

    /**
     * The items are built anew, and what was open is opened again by the items down to it.
     */
    @RequiredUIAccess
    CompletableFuture<?> rootsChanged() {
        List<List<ServiceViewItem>> expandedPaths = collectExpandedPaths();
        return myTree.refreshAll().thenRun(() -> {
            for (List<ServiceViewItem> path : expandedPaths) {
                promiseExpand(path);
            }
        });
    }

    @RequiredUIAccess
    List<List<ServiceViewItem>> collectExpandedPaths() {
        return myTree.getExpandedPaths().stream().map(path -> path.stream().map(TreeNode::getValue).filter(item -> item != null).toList()).filter(path -> !path.isEmpty()).toList();
    }

    /**
     * Opens the items of the path down to its last one, which is selected - {@code TreeUtil.promiseSelect} of the
     * swing tree.
     */
    CompletableFuture<TreeNode<ServiceViewItem>> promiseSelect(List<ServiceViewItem> path) {
        return promiseNode(path).thenApply(node -> {
            if (node != null) {
                myUIAccess.give(() -> myTree.select(node));
            }
            return node;
        });
    }

    CompletableFuture<TreeNode<ServiceViewItem>> promiseExpand(List<ServiceViewItem> path) {
        return promiseNode(path).thenCompose(node -> {
            CompletableFuture<TreeNode<ServiceViewItem>> result = new CompletableFuture<>();
            if (node == null) {
                result.complete(null);
            }
            else {
                myUIAccess.give(() -> myTree.expand(node).whenComplete((o, e) -> result.complete(node)));
            }
            return result;
        });
    }

    /**
     * The node of the last item of the path - the ones before it are opened on the way, since a level is built only
     * when it is opened.
     */
    private CompletableFuture<TreeNode<ServiceViewItem>> promiseNode(List<ServiceViewItem> path) {
        CompletableFuture<TreeNode<ServiceViewItem>> result = new CompletableFuture<>();
        myUIAccess.give(() -> {
            TreeNode<ServiceViewItem> root = myTree.getRootNode();
            if (root == null || path.isEmpty()) {
                result.complete(null);
                return;
            }
            findNode(root, path, 0, result);
        });
        return result;
    }

    private void findNode(TreeNode<ServiceViewItem> parent, List<ServiceViewItem> path, int index, CompletableFuture<TreeNode<ServiceViewItem>> result) {
        ServiceViewItem item = path.get(index);
        parent.findChild(item::equals).whenComplete((child, error) -> {
            if (child == null) {
                result.complete(null);
                return;
            }
            if (index == path.size() - 1) {
                result.complete(child);
                return;
            }

            myUIAccess.give(() -> myTree.expand(child).whenComplete((o, e) -> findNode(child, path, index + 1, result)));
        });
    }

    /**
     * The presentation of an item the way the swing {@code NodeRenderer} draws it.
     */
    private static final class UnifiedServiceViewTreeCellRenderer implements BiConsumer<ServiceViewItem, TextItemPresentation> {
        private final Supplier<ServiceViewOptions> myViewOptions;

        private UnifiedServiceViewTreeCellRenderer(Supplier<ServiceViewOptions> viewOptions) {
            myViewOptions = viewOptions;
        }

        @Override
        public void accept(ServiceViewItem item, TextItemPresentation presentation) {
            // the unified tree tells no state of the row it renders
            ServiceViewItemState itemState = new ServiceViewItemState(false, false, false, false);
            ItemPresentation itemPresentation = item.getItemPresentation(myViewOptions.get(), itemState);

            presentation.withIcon(itemPresentation.getIcon());

            if (!(itemPresentation instanceof PresentationData data)) {
                presentation.append(LocalizeValue.ofNullable(itemPresentation.getPresentableText()));
                return;
            }

            ColorValue forced = data.getForcedTextForeground();
            List<PresentableNodeDescriptor.ColoredFragment> fragments = data.getColoredText();
            if (fragments.isEmpty()) {
                String text = data.getPresentableText();
                presentation.append(LocalizeValue.ofNullable(StringUtil.isEmpty(text) ? item.toString() : text), toTextAttribute(null, forced));

                String location = data.getLocationString();
                if (!StringUtil.isEmpty(location)) {
                    presentation.append(LocalizeValue.of(data.getLocationPrefix() + location + data.getLocationSuffix()), TextAttribute.GRAYED);
                }
                return;
            }

            for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
                presentation.append(fragment.getText(), toTextAttribute(fragment.getAttributes(), forced));
            }
        }

        private static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes, @Nullable ColorValue forced) {
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
            return new TextAttribute(style, foreground == null ? forced : foreground, background);
        }
    }
}
