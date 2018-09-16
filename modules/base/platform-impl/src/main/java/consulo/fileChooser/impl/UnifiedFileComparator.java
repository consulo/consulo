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
package consulo.fileChooser.impl;

import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.TreeNode;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public class UnifiedFileComparator implements Comparator<TreeNode<FileElement>> {
  private static final UnifiedFileComparator INSTANCE = new UnifiedFileComparator();

  private UnifiedFileComparator() {
  }

  public static UnifiedFileComparator getInstance() {
    return INSTANCE;
  }

  @Override
  public int compare(TreeNode<FileElement> fileElement1, TreeNode<FileElement> fileElement2) {
    int weight1 = getWeight(fileElement1);
    int weight2 = getWeight(fileElement2);

    if (weight1 != weight2) {
      return weight1 - weight2;
    }

    return fileElement1.getValue().getName().compareToIgnoreCase(fileElement2.getValue().getName());
  }

  private static int getWeight(TreeNode<FileElement> descriptor) {
    assert descriptor.getValue() != null;

    VirtualFile file = descriptor.getValue().getFile();
    return file == null || file.isDirectory() ? 0 : 1;
  }
}
