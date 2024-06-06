package consulo.remoteServer.impl.internal.ui;

import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.project.ui.view.tree.AbstractTreeStructureBase;
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
