/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.dataContext.DataContext;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.tree.TreeUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;

class ChangesBrowserNodeCopyProvider implements CopyProvider {

  @Nonnull
  private final JTree myTree;

  ChangesBrowserNodeCopyProvider(@Nonnull JTree tree) {
    myTree = tree;
  }

  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    return myTree.getSelectionPaths() != null;
  }

  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }

  public void performCopy(@Nonnull DataContext dataContext) {
    List<TreePath> paths = ContainerUtil.sorted(Arrays.asList(ObjectUtil.assertNotNull(myTree.getSelectionPaths())), TreeUtil.getDisplayOrderComparator(myTree));
    CopyPasteManager.getInstance().setContents(new StringSelection(StringUtil.join(paths, path -> {
      Object node = path.getLastPathComponent();
      if (node instanceof ChangesBrowserNode) {
        return ((ChangesBrowserNode)node).getTextPresentation();
      }
      else {
        return node.toString();
      }
    }, "\n")));
  }
}
