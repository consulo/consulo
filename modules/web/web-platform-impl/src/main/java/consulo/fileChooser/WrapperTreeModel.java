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
package consulo.fileChooser;

import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.fileChooser.ex.FileNodeDescriptor;
import com.intellij.openapi.fileChooser.impl.FileTreeStructure;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WrapperTreeModel implements TreeModel<FileElement> {
  private FileTreeStructure myFileTreeStructure;

  public WrapperTreeModel(FileTreeStructure fileTreeStructure) {
    myFileTreeStructure = fileTreeStructure;
  }

  public FileElement getRootElement() {
    return (FileElement)myFileTreeStructure.getRootElement();
  }

  @Override
  public void fetchChildren(@NotNull Function<FileElement, TreeNode<FileElement>> nodeFactory, @Nullable FileElement parentValue) {
    for (Object o : myFileTreeStructure.getChildElements(parentValue)) {
      FileElement element = (FileElement)o;
      TreeNode<FileElement> apply = nodeFactory.apply(element);

      apply.setLeaf(!(element.getFile()).isDirectory());
      apply.setRender((fileElement, itemPresentation) -> {
        FileNodeDescriptor descriptor = (FileNodeDescriptor)myFileTreeStructure.createDescriptor(element, null);

        descriptor.update();

        itemPresentation.append(descriptor.getName());
        try {
          itemPresentation.setIcon((consulo.ui.image.Image)descriptor.getIcon());
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
  }
}
