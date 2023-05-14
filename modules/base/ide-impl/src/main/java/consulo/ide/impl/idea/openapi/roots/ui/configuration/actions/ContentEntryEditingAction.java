/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since Oct 14, 2003
 */
public abstract class ContentEntryEditingAction extends ToggleAction implements DumbAware {
  protected final JTree myTree;

  protected ContentEntryEditingAction(JTree tree) {
    this(tree, null, null);
  }

  protected ContentEntryEditingAction(JTree tree, @Nullable String text, @Nullable Image image) {
    super(text, null, image);
    myTree = tree;
    getTemplatePresentation().setEnabled(true);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(true);
    final VirtualFile[] files = getSelectedFiles();
    if (files.length == 0) {
      presentation.setEnabled(false);
      return;
    }
    for (VirtualFile file : files) {
      if (file == null || !file.isDirectory()) {
        presentation.setEnabled(false);
        break;
      }
    }
  }

  @Nonnull
  protected final VirtualFile[] getSelectedFiles() {
    final TreePath[] selectionPaths = myTree.getSelectionPaths();
    if (selectionPaths == null) {
      return VirtualFile.EMPTY_ARRAY;
    }
    final List<VirtualFile> selected = new ArrayList<VirtualFile>();
    for (TreePath treePath : selectionPaths) {
      VirtualFile file = FileSystemTreeImpl.getVirtualFile(treePath);
      if (file != null) {
        selected.add(file);
      }
    }
    return selected.toArray(new VirtualFile[selected.size()]);
  }
}
