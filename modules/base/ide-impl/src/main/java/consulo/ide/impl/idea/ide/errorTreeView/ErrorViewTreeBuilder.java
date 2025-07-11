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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public class ErrorViewTreeBuilder extends AbstractTreeBuilder{
  public ErrorViewTreeBuilder(JTree tree, DefaultTreeModel treeModel, AbstractTreeStructure treeStructure) {
    super(tree, treeModel, treeStructure, null);
  }

  public void updateFromRoot() {
    if (isDisposed()) return;
    getUpdater().cancelAllRequests();
    super.updateFromRoot();
  }

  public void updateTree() {
    if (isDisposed()) return;
    getUpdater().addSubtreeToUpdate(getRootNode());
  }

  public void updateTree(Runnable runAferUpdate) {
    if (isDisposed()) return;
    getUpdater().runAfterUpdate(runAferUpdate);
    updateTree();
  }

  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    final ErrorTreeElement element = (ErrorTreeElement)nodeDescriptor.getElement();
    if (element instanceof GroupingElement) {
      return ((ErrorViewStructure)getTreeStructure()).getChildCount((GroupingElement)element) > 0;
    }
    return false;
  }

  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return nodeDescriptor.getParentDescriptor() == null || nodeDescriptor.getElement() instanceof GroupingElement;
  }
}

