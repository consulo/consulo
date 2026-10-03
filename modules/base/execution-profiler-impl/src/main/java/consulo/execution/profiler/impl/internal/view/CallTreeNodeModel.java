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
package consulo.execution.profiler.impl.internal.view;

import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.impl.internal.calltree.CallTreeNode;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeNodeModel implements TreeModel<CallTreeNode> {
    private final BaseCallStackElementRenderer myRenderer;
    private final ProfilerNavigator myNavigator;

    public CallTreeNodeModel(BaseCallStackElementRenderer renderer, ProfilerNavigator navigator) {
        myRenderer = renderer;
        myNavigator = navigator;
    }

    @Override
    public void buildChildren(Function<CallTreeNode, TreeNode<CallTreeNode>> nodeFactory, @Nullable CallTreeNode parentValue) {
        if (parentValue == null) {
            return;
        }

        for (CallTreeNode child : parentValue.getChildren()) {
            TreeNode<CallTreeNode> node = nodeFactory.apply(child);
            node.setLeaf(child.getChildren().isEmpty());
            node.setRenderer(this::render);
        }
    }

    @Override
    @RequiredUIAccess
    public boolean onDoubleClick(Tree<CallTreeNode> tree, TreeNode<CallTreeNode> node) {
        CallTreeNode value = node.getValue();
        BaseCallStackElement element = value == null ? null : value.getElement();
        if (element == null || !element.isNavigatable()) {
            return true;
        }

        myNavigator.navigate(element);
        return false;
    }

    private void render(CallTreeNode node, TextItemPresentation presentation) {
        BaseCallStackElement element = node.getElement();
        if (element == null) {
            presentation.append(LocalizeValue.localizeTODO("All threads"));
        }
        else {
            myRenderer.render(element, presentation);
        }
    }
}
