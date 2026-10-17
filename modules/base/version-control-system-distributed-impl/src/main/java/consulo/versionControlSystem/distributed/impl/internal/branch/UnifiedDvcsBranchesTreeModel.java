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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupStepBase;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeSeparator;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
class UnifiedDvcsBranchesTreeModel implements TreeModel<Object> {
    private final DvcsBranchesTreePopupStepBase myStep;

    UnifiedDvcsBranchesTreeModel(DvcsBranchesTreePopupStepBase step) {
        myStep = step;
    }

    @Override
    public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
        DvcsBranchesTreeModel model = myStep.getTreeModel();
        Object parent = parentValue == null ? model.getRoot() : parentValue;

        for (Object child : model.getChildNodes(parent)) {
            TreeNode<Object> node = nodeFactory.apply(child);
            node.setLeaf(child instanceof DvcsBranchesTreeSeparator || model.isLeaf(child));
            node.setRenderer(myStep::renderNode);
        }
    }

    @Override
    public boolean onDoubleClick(Tree<Object> tree, TreeNode<Object> node) {
        return false;
    }
}
