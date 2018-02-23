/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.ui;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.application.ReadAction;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class TreeStructureWrappenModel<T> implements TreeModel<T> {
  private AbstractTreeStructure myStructure;

  public TreeStructureWrappenModel(AbstractTreeStructure structure) {
    myStructure = structure;
  }

  @Nullable
  public T getRootElement() {
    return (T)myStructure.getRootElement();
  }

  @Override
  public boolean isNeedBuildChildrenBeforeOpen(@Nonnull TreeNode<T> node) {
    return myStructure.isToBuildChildrenInBackground(node.getValue());
  }

  @Override
  public void fetchChildren(@Nonnull Function<T, TreeNode<T>> nodeFactory, @Nullable T parentValue) {
    for (Object o : ReadAction.compute(() -> myStructure.getChildElements(parentValue))) {
      T element = (T)o;
      TreeNode<T> apply = nodeFactory.apply(element);

      apply.setLeaf(o instanceof AbstractTreeNode && !((AbstractTreeNode)o).isAlwaysShowPlus());


      apply.setRender((fileElement, itemPresentation) -> {
        NodeDescriptor descriptor = myStructure.createDescriptor(element, null);

        descriptor.update();

        itemPresentation.append(descriptor.toString());
        try {
          ReadAction.run(() -> itemPresentation.setIcon((consulo.ui.image.Image)descriptor.getIcon()));
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
  }
}
