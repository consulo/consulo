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
package consulo.ui.ex.tree;

import consulo.application.ReadAction;
import consulo.application.util.function.ThrowableComputable;
import consulo.dataContext.DataManager;
import consulo.logging.Logger;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.event.details.InputDetails;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class TreeStructureWrappenModel<T> implements TreeModel<T> {
    private static final Logger LOG = Logger.getInstance(TreeStructureWrappenModel.class);

    private AbstractTreeStructure myStructure;

    public TreeStructureWrappenModel(AbstractTreeStructure structure) {
        myStructure = structure;
    }

    public @Nullable T getRootElement() {
        return (T) myStructure.getRootElement();
    }

    @Override
    public boolean onDoubleClick(Tree<T> tree, TreeNode<T> node, @Nullable InputDetails inputDetails) {
        if (node.getValue() instanceof SimpleNode simpleNode) {
            return !simpleNode.handleDoubleClickOrEnter(DataManager.getInstance().getDataContext(tree), inputDetails);
        }
        return true;
    }

    @Override
    public boolean isNeedBuildChildrenBeforeOpen(TreeNode<T> node) {
        return myStructure.isToBuildChildrenInBackground(node.getValue());
    }

    private boolean isLeaf(Object element) {
        LeafState leafState = element instanceof LeafState.Supplier supplier ? supplier.getLeafState() : LeafState.DEFAULT;

        return switch (leafState) {
            case ALWAYS -> true;
            case NEVER, ASYNC -> false;
            case DEFAULT -> ReadAction.compute(() -> myStructure.getChildElements(element)).length == 0;
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void buildChildren(Function<T, TreeNode<T>> nodeFactory, @Nullable T parentValue) {
        ThrowableComputable<Object[], RuntimeException> action = () -> myStructure.getChildElements(parentValue);

        for (Object o : ReadAction.compute(action)) {
            T element = (T) o;
            TreeNode<T> apply = nodeFactory.apply(element);

            // the order of a level is decided on the descriptors of that level, and one which was never updated
            // carries no presentation yet - its name is null, and a comparator ordering by name cannot tell two
            // of them apart. the awt tree updates them as it builds, for the same reason
            if (o instanceof NodeDescriptor descriptor) {
                ReadAction.compute(() -> descriptor.update());
            }

            apply.setLeaf(isLeaf(o));

            apply.setRenderer((fileElement, itemPresentation) -> {
                NodeDescriptor descriptor = myStructure.createDescriptor(element, null);

                descriptor.update();

                if (descriptor instanceof PresentableNodeDescriptor<?> presentable) {
                    SimpleTreeModel.renderPresentation(itemPresentation, presentable, descriptor);
                }
                else {
                    itemPresentation.append(descriptor.toString());
                }

                try {
                    ReadAction.compute(() -> itemPresentation.withIcon(SimpleTreeModel.iconOf(descriptor)));
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            });
        }
    }
}
