package consulo.ide.impl.idea.remoteServer.impl.runtime.ui.tree;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ide.impl.idea.ide.util.treeView.AbstractTreeStructureBase;
import consulo.ui.ex.tree.IndexComparator;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class TreeBuilderBase extends AbstractTreeBuilder {
  public TreeBuilderBase(JTree tree, AbstractTreeStructureBase structure, DefaultTreeModel treeModel) {
    super(tree, treeModel, structure, IndexComparator.INSTANCE);
    initRootNode();
  }
}
