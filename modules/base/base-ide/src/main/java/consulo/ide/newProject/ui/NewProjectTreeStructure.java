/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.newProject.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.ui.SimpleTextAttributes;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.newProject.node.NewModuleContextGroup;
import consulo.ide.newProject.node.NewModuleContextNode;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-06-02
 */
public class NewProjectTreeStructure extends AbstractTreeStructure {
  private final NewModuleContext myContext;

  public NewProjectTreeStructure(NewModuleContext context) {
    myContext = context;
  }

  @Nonnull
  @Override
  public Object getRootElement() {
    return myContext;
  }

  @Nonnull
  @Override
  public Object[] getChildElements(@Nonnull Object element) {
    if (element instanceof NewModuleContextGroup) {
      return ((NewModuleContextGroup)element).getAll().toArray();
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Nullable
  @Override
  public Object getParentElement(@Nonnull Object element) {
    return null;
  }

  @Nonnull
  @Override
  public NodeDescriptor createDescriptor(@Nonnull Object element, @Nullable NodeDescriptor parentDescriptor) {
    return new PresentableNodeDescriptor(null, parentDescriptor) {

      @Override
      protected void update(PresentationData presentation) {
        if (element instanceof NewModuleContextNode) {
          boolean isGroup = element instanceof NewModuleContextGroup;
          presentation.addText(((NewModuleContextNode)element).getName().getValue(), isGroup ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
          presentation.setIcon(((NewModuleContextNode)element).getImage());
        }
      }

      @Override
      public Object getElement() {
        return element;
      }
    };
  }

  @Override
  public void commit() {

  }

  @Override
  public boolean hasSomethingToCommit() {
    return false;
  }
}
