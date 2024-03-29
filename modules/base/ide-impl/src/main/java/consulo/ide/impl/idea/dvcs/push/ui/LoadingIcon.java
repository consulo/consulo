/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.push.ui;

import consulo.logging.Logger;
import consulo.ui.ex.awt.internal.ImageLoader;
import consulo.ide.impl.idea.util.ui.JBImageIcon;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.ImageObserver;

class LoadingIcon extends JBImageIcon {

  //todo fix double size animated icons
  private static final String LOADING_ICON = "/icons/loading.gif";
  private static final Logger LOG = Logger.getInstance(LoadingIcon.class);

  LoadingIcon(@Nonnull Image image) {
    super(image);
  }

  @Nonnull
  static LoadingIcon create(int width, int height) {
    Image image = ImageLoader.loadFromResource(LOADING_ICON);
    if (image == null) {
      LOG.error("Couldn't load image: " + LOADING_ICON);
      return createEmpty(width, height);
    }
    return new LoadingIcon(image);
  }

  @Nonnull
  static LoadingIcon createEmpty(int width, int height) {
    return new LoadingIcon(UIUtil.createImage(width, height, Transparency.TRANSLUCENT));
  }

  void setObserver(@Nonnull JTree tree, @Nonnull TreeNode treeNode) {
    setImageObserver(new NodeImageObserver(tree, treeNode));
  }

  private static class NodeImageObserver implements ImageObserver {
    @Nonnull
    private final JTree myTree;
    @Nonnull
    private final DefaultTreeModel myModel;
    @Nonnull
    private final TreeNode myNode;

    NodeImageObserver(@Nonnull JTree tree, @Nonnull TreeNode node) {
      myTree = tree;
      myModel = (DefaultTreeModel)tree.getModel();
      myNode = node;
    }

    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
      if (myNode instanceof RepositoryNode && !((RepositoryNode)myNode).isLoading()) return false;
      if ((flags & (FRAMEBITS | ALLBITS)) != 0) {
        TreeNode[] pathToRoot = myModel.getPathToRoot(myNode);
        if (pathToRoot != null) {
          TreePath path = new TreePath(pathToRoot);
          Rectangle rect = myTree.getPathBounds(path);
          if (rect != null) {
            myTree.repaint(rect);
          }
        }
      }
      return (flags & (ALLBITS | ABORT)) == 0;
    }
  }
}
