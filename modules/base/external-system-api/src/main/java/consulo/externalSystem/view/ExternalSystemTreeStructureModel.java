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
package consulo.externalSystem.view;

import consulo.ui.TreeNode;
import consulo.ui.ex.tree.SimpleNode;
import consulo.ui.ex.tree.SimpleTreeModel;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Tree model over {@link ExternalProjectsStructure}. Keeps the handle of every node it builds, so that the structure can refresh or
 * select a node it holds without walking the tree.
 *
 * @author VISTALL
 */
public class ExternalSystemTreeStructureModel extends SimpleTreeModel<SimpleNode> {
    private final Map<SimpleNode, TreeNode<SimpleNode>> myHandles = new ConcurrentHashMap<>();

    public ExternalSystemTreeStructureModel(ExternalProjectsStructure structure) {
        super(() -> (SimpleNode)structure.getRootElement());
    }

    @Override
    public void buildChildren(Function<SimpleNode, TreeNode<SimpleNode>> nodeFactory, @Nullable SimpleNode parentValue) {
        super.buildChildren(node -> {
            TreeNode<SimpleNode> handle = nodeFactory.apply(node);
            myHandles.put(node, handle);
            return handle;
        }, parentValue);
    }

    public @Nullable TreeNode<SimpleNode> getHandle(SimpleNode node) {
        return myHandles.get(node);
    }

    public Map<SimpleNode, TreeNode<SimpleNode>> getHandles() {
        return myHandles;
    }
}
