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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackageSupportProvider;
import consulo.roots.ContentFolderScopes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class ContentEntryTreeCellRenderer extends NodeRenderer {
  protected final ContentEntryTreeEditor myTreeEditor;

  public ContentEntryTreeCellRenderer(@Nonnull final ContentEntryTreeEditor treeEditor) {
    myTreeEditor = treeEditor;
  }

  @RequiredUIAccess
  @Override
  public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

    final ContentEntryEditor editor = myTreeEditor.getContentEntryEditor();
    if (editor != null) {
      final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (userObject instanceof NodeDescriptor) {
        final Object element = ((NodeDescriptor)userObject).getElement();
        if (element instanceof FileElement) {
          final VirtualFile file = ((FileElement)element).getFile();
          if (file != null && file.isDirectory()) {
            final ContentEntry contentEntry = editor.getContentEntry();
            setIcon(updateIcon(contentEntry, file, getIcon()));
          }
        }
      }
    }
  }


  protected Image updateIcon(final ContentEntry entry, final VirtualFile file, Image originalIcon) {
    Image icon = originalIcon;
    VirtualFile currentRoot = null;
    for (ContentFolder contentFolder : entry.getFolders(ContentFolderScopes.all())) {
      final VirtualFile contentPath = contentFolder.getFile();
      if (file.equals(contentPath)) {
        icon = contentFolder.getType().getIcon(contentFolder.getProperties());
      }
      else if (contentPath != null && VfsUtilCore.isAncestor(contentPath, file, true)) {
        if (currentRoot != null && VfsUtilCore.isAncestor(contentPath, currentRoot, false)) {
          continue;
        }

        boolean hasSupport = false;
        for (ModuleExtension moduleExtension : myTreeEditor.getContentEntryEditor().getModel().getExtensions()) {
          for (PsiPackageSupportProvider supportProvider : PsiPackageSupportProvider.EP_NAME.getExtensionList()) {
            if (supportProvider.isSupported(moduleExtension)) {
              hasSupport = true;
              break;
            }
          }
        }
        icon = hasSupport ? contentFolder.getType().getChildDirectoryIcon(null) : AllIcons.Nodes.TreeOpen;
        currentRoot = contentPath;
      }
    }
    return icon;
  }
}
