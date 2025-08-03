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
package consulo.ide.impl.fileChooser;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileTreeStructure;
import consulo.project.Project;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.ex.tree.TreeStructureWrappenModel;

import jakarta.annotation.Nullable;
import java.util.Comparator;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class FileTreeComponent {
  public static Tree<FileElement> create(Project project, FileChooserDescriptor descriptor, Disposable uiDisposable) {
    FileTreeStructure fileTreeStructure = new FileTreeStructure(project, descriptor) {
      @Override
      public boolean isToBuildChildrenInBackground(Object element) {
        return false;
      }
    };
    fileTreeStructure.showHiddens(true);
    TreeStructureWrappenModel<FileElement> treeStructureWrappenModel = new TreeStructureWrappenModel<>(fileTreeStructure) {
      @Nullable
      @Override
      public Comparator<TreeNode<FileElement>> getNodeComparator() {
        return UnifiedFileComparator.getInstance();
      }
    };
    return Tree.create(treeStructureWrappenModel.getRootElement(), treeStructureWrappenModel, uiDisposable);
  }
}
