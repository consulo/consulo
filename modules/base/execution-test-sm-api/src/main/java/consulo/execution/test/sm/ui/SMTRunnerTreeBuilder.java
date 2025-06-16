/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.test.sm.ui;

import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.ui.AbstractTestTreeBuilder;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerTreeBuilder extends AbstractTestTreeBuilder {
    public SMTRunnerTreeBuilder(JTree tree, SMTRunnerTreeStructure structure) {
        super(tree, new DefaultTreeModel(new DefaultMutableTreeNode(structure.getRootElement())), structure, IndexComparator.INSTANCE);

        setCanYieldUpdate(true);
        initRootNode();
    }

    public SMTRunnerTreeStructure getSMRunnerTreeStructure() {
        return ((SMTRunnerTreeStructure) getTreeStructure());
    }

    public void updateTestsSubtree(SMTestProxy parentTestProxy) {
        queueUpdateFrom(parentTestProxy, false, true);
    }

    @Override
    protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        AbstractTreeStructure treeStructure = getTreeStructure();
        Object rootElement = treeStructure.getRootElement();
        Object nodeElement = nodeDescriptor.getElement();

        if (nodeElement == rootElement) {
            return true;
        }

        if (((SMTestProxy) nodeElement).getParent() == rootElement && ((SMTestProxy) rootElement).getChildren().size() == 1) {
            return true;
        }
        return false;
    }

    /**
     * for java unit tests
     */
    public void performUpdate() {
        getUpdater().performUpdate();
    }
}
