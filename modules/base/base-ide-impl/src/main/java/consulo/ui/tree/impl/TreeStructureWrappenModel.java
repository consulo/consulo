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
package consulo.ui.tree.impl;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.application.AccessRule;
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
  public void buildChildren(@Nonnull Function<T, TreeNode<T>> nodeFactory, @Nullable T parentValue) {
    ThrowableComputable<Object[],RuntimeException> action = () -> myStructure.getChildElements(parentValue);
    for (Object o : AccessRule.read(action)) {
      T element = (T)o;
      TreeNode<T> apply = nodeFactory.apply(element);

      apply.setLeaf(o instanceof AbstractTreeNode && !((AbstractTreeNode)o).isAlwaysShowPlus());


      apply.setRender((fileElement, itemPresentation) -> {
        NodeDescriptor descriptor = myStructure.createDescriptor(element, null);

        descriptor.update();

        itemPresentation.append(descriptor.toString());
        try {
          AccessRule.read(() -> itemPresentation.withIcon(descriptor.getIcon()));
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
  }
}
