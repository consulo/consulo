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
package consulo.ide.impl.newProject.ui;

import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ide.newModule.NewModuleContext;
import consulo.ide.newModule.NewModuleContextGroup;
import consulo.ide.newModule.NewModuleContextNode;
import consulo.util.collection.ArrayUtil;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-06-02
 */
public class NewProjectTreeStructure extends AbstractTreeStructure {
  private final NewModuleContext myContext;

  public NewProjectTreeStructure(NewModuleContext context) {
    myContext = context;
  }

  
  @Override
  public Object getRootElement() {
    return myContext;
  }

  
  @Override
  public Object[] getChildElements(Object element) {
    if (element instanceof NewModuleContextGroup) {
      return ((NewModuleContextGroup)element).getAll().toArray();
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public @Nullable Object getParentElement(Object element) {
    return null;
  }

  
  @Override
  public NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
    return new PresentableNodeDescriptor(parentDescriptor) {

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
