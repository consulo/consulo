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
package consulo.desktop.awt.versionSystemControl.ui;

import consulo.ui.ex.awt.tree.AbstractTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;

import javax.swing.tree.TreePath;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class DvcsBranchesSwingTreeModel extends AbstractTreeModel {
    private final DvcsBranchesTreeModel myModel;
    private final Runnable myChangeListener = () -> treeStructureChanged(new TreePath(getRoot()), null, null);

    public DvcsBranchesSwingTreeModel(DvcsBranchesTreeModel model) {
        myModel = model;
        myModel.addChangeListener(myChangeListener);
    }

    public DvcsBranchesTreeModel getModel() {
        return myModel;
    }

    @Override
    public Object getRoot() {
        return myModel.getRoot();
    }

    @Override
    public Object getChild(Object parent, int index) {
        return myModel.getChildNodes(parent).get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return myModel.getChildNodes(parent).size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return myModel.getChildNodes(parent).indexOf(child);
    }

    @Override
    public boolean isLeaf(Object node) {
        return myModel.isLeaf(node);
    }

    @Override
    public void dispose() {
        myModel.removeChangeListener(myChangeListener);
        super.dispose();
    }
}
