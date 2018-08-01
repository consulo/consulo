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
package com.intellij.ui;

import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.containers.Convertor;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.TreePath;

/**
 * @author yole
 */
@Singleton
public class TreeUIHelperImpl extends TreeUIHelper {
  @Override
  public void installToolTipHandler(final JTree tree) {
    if (tree instanceof Tree) return;
    new TreeExpandableItemsHandler(tree);
  }

  @Override
  public void installToolTipHandler(final JTable table) {
    if (table instanceof JBTable) return;
    new TableExpandableItemsHandler(table);
  }

  @Override
  public void installToolTipHandler(JList list) {
    if (list instanceof JBList) return;
    new ListExpandableItemsHandler(list);
  }

  @Override
  public void installEditSourceOnDoubleClick(final JTree tree) {
    EditSourceOnDoubleClickHandler.install(tree);
  }

  @Override
  public void installTreeSpeedSearch(final JTree tree) {
    new TreeSpeedSearch(tree);
  }

  @Override
  public void installTreeSpeedSearch(JTree tree, Convertor<TreePath, String> convertor, boolean canExpand) {
    new TreeSpeedSearch(tree, convertor, canExpand);
  }

  @Override
  public void installListSpeedSearch(JList list) {
    new ListSpeedSearch(list);
  }

  @Override
  public void installListSpeedSearch(JList list, Convertor<Object, String> convertor) {
    new ListSpeedSearch(list, convertor);
  }

  @Override
  public void installEditSourceOnEnterKeyHandler(final JTree tree) {
    EditSourceOnEnterKeyHandler.install(tree);
  }

  @Override
  public void installSmartExpander(final JTree tree) {
    SmartExpander.installOn(tree);
  }

  @Override
  public void installSelectionSaver(final JTree tree) {
    SelectionSaver.installOn(tree);
  }
}