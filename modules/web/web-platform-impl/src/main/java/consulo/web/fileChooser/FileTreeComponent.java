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
package consulo.web.fileChooser;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.fileChooser.impl.FileTreeStructure;
import com.intellij.openapi.project.Project;
import consulo.ui.Tree;
import consulo.web.ui.TreeStructureWrappenModel;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class FileTreeComponent {
  public static Tree<FileElement> create(Project project, FileChooserDescriptor descriptor) {
    TreeStructureWrappenModel<FileElement> treeStructureWrappenModel = new TreeStructureWrappenModel<>(new FileTreeStructure(project, descriptor));
    return Tree.create(treeStructureWrappenModel.getRootElement(), treeStructureWrappenModel);
  }
}
