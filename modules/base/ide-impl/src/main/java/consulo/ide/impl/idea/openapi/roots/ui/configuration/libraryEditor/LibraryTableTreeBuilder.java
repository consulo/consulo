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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

class LibraryTableTreeBuilder extends AbstractTreeBuilder {
  public LibraryTableTreeBuilder(JTree tree, DefaultTreeModel treeModel, AbstractTreeStructure treeStructure) {
    super(tree, treeModel, treeStructure, IndexComparator.INSTANCE);
    initRootNode();
  }

  @Override
  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    return false;
  }

  @Override
  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    Object element = nodeDescriptor.getElement();
    Object rootElement = getTreeStructure().getRootElement();
    return rootElement.equals(element) || element instanceof OrderRootTypeElement;
  }

  @Override
  protected boolean isSmartExpand() {
    return false;
  }
}
