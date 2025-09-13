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
package consulo.versionControlSystem.impl.internal.update;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.VcsBundle;
import consulo.virtualFileSystem.VirtualFile;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSetBase;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author lesya
 */
public abstract class AbstractTreeNode extends DefaultMutableTreeNode {
  protected static final ArrayList<File> EMPTY_FILE_ARRAY = new ArrayList<File>();
  DefaultTreeModel myTreeModel;
  private JTree myTree;
  private String myErrorText;
  protected SimpleTextAttributes myFilterAttributes;

  public void setTree(JTree tree) {
    myTree = tree;
    if (children == null) return;
    for (Object aChildren : children) {
      AbstractTreeNode node = (AbstractTreeNode)aChildren;
      node.setTree(tree);
    }
  }

  public void setTreeModel(DefaultTreeModel treeModel) {
    myTreeModel = treeModel;
    if (children == null) return;
    for (Object aChildren : children) {
      AbstractTreeNode node = (AbstractTreeNode)aChildren;
      node.setTreeModel(treeModel);
    }
  }

  public void setErrorText(String errorText) {
    myErrorText = errorText;
  }

  public String getErrorText() {
    return myErrorText;
  }

  protected boolean acceptFilter(@Nullable Pair<PackageSetBase, NamedScopesHolder> filter, boolean showOnlyFilteredItems) {
    boolean apply = false;
    if (children != null && filter != null) {
      for (Iterator it = children.iterator(); it.hasNext(); ) {
        AbstractTreeNode node = (AbstractTreeNode)it.next();
        if (node.acceptFilter(filter, showOnlyFilteredItems)) {
          apply = true;
        }
        else if (showOnlyFilteredItems) {
          if (node instanceof Disposable) {
            Disposer.dispose((Disposable)node);
          }
          it.remove();
        }
      }
      applyFilter(apply);
    }
    return apply;
  }

  protected void applyFilter(boolean apply) {
    myFilterAttributes = apply ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : null;
  }

  protected DefaultTreeModel getTreeModel() {
    return myTreeModel;
  }

  public JTree getTree() {
    return myTree;
  }

  public AbstractTreeNode() {
  }

  public String getText() {
    StringBuilder result = new StringBuilder();
    result.append(getName());
    if (showStatistics()) {
      result.append(" (");
      result.append(getStatistics(getItemsCount()));
      result.append(")");
    }
    return result.toString();
  }

  private static String getStatistics(int itemsCount) {
    return VcsBundle.message("update.tree.node.size.statistics", itemsCount);
  }

  @Nonnull
  protected abstract String getName();

  protected abstract int getItemsCount();

  protected abstract boolean showStatistics();

  @NonNls
  public abstract Image getIcon();

  @Nonnull
  public abstract Collection<VirtualFile> getVirtualFiles();

  @Nonnull
  public abstract Collection<File> getFiles();

  @Nonnull
  public abstract SimpleTextAttributes getAttributes();

  public abstract boolean getSupportsDeletion();
}
