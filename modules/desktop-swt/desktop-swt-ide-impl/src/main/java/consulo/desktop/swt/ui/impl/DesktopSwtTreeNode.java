/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.desktop.swt.ui.impl.image.DesktopSwtImage;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import org.eclipse.swt.widgets.TreeItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtTreeNode<E> implements TreeNode<E> {
  private BiConsumer<E, TextItemPresentation> myRender;

  private final E myValue;

  private TreeItem myTreeItem;

  private boolean myLeaf;

  public DesktopSwtTreeNode(E value) {
    myValue = value;
  }

  public void setTreeItem(TreeItem treeItem) {
    myTreeItem = treeItem;
  }

  public TreeItem getTreeItem() {
    return myTreeItem;
  }

  public void render() {
    if (myRender == null) {
      myRender = (e, presentation) -> {
        if (e == null) {
          presentation.append("");
        }
        else {
          presentation.append(e.toString());
        }
      };
    }

    DesktopSwtTextItemPresentation presentation = new DesktopSwtTextItemPresentation();
    myRender.accept(myValue, presentation);
    myTreeItem.setText(presentation.toString());

    consulo.ui.image.Image uiImage = presentation.getImage();
    if(uiImage != null) {
      myTreeItem.setImage(DesktopSwtImage.toSWTImage(uiImage));
    }
  }

  @Override
  public void setRender(@Nonnull BiConsumer<E, TextItemPresentation> render) {
    myRender = render;
  }

  @Override
  public void setLeaf(boolean leaf) {
    myLeaf = leaf;
  }

  @Override
  public boolean isLeaf() {
    return myLeaf;
  }

  @Nullable
  @Override
  public E getValue() {
    return myValue;
  }
}
