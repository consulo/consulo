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

import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileTypes.impl.VfsIconUtil;
import consulo.ui.Component;
import consulo.ui.Components;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class FileTreeComponent {
  public static Component create() {
    TreeModel<VirtualFile> model = new TreeModel<VirtualFile>() {
      @Override
      public void fetchChildren(@NotNull Function<VirtualFile, TreeNode<VirtualFile>> nodeFactory, @Nullable VirtualFile parentValue) {
        VirtualFile[] children = null;
        if (parentValue == null) {
          children = getFileSystemRoots();
        }
        else {
          children = parentValue.getChildren();
        }

        Arrays.sort(children, (o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName()));
        for (VirtualFile child : children) {
          TreeNode<VirtualFile> apply = nodeFactory.apply(child);
          apply.setLeaf(!(child.isDirectory()));
          apply.setRender((virtualFile, itemPresentation) -> {
            Image img = null;
            try {
              Icon icon = VfsIconUtil.getIcon(virtualFile, Iconable.ICON_FLAG_READ_STATUS, null);
              img = (Image)icon;
            }
            catch (Exception e) {
              try {
                img = (Image)VirtualFilePresentation.getIcon(virtualFile);
              }
              catch (Exception e1) {
                //
              }
            }

            // swing resource
            if(img instanceof ImageIcon) {
              img = null;
            }
            if(img != null) {
              itemPresentation.setIcon(img);
            }
            itemPresentation.append(virtualFile.getName());
          });
        }
      }
    };

    return Components.tree(null, model);
  }

  private static VirtualFile[] getFileSystemRoots() {
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    final Set<VirtualFile> roots = new HashSet<>();
    final File[] ioRoots = File.listRoots();
    if (ioRoots != null) {
      for (final File root : ioRoots) {
        final String path = FileUtil.toSystemIndependentName(root.getAbsolutePath());
        final VirtualFile file = localFileSystem.findFileByPath(path);
        if (file != null) {
          roots.add(file);
        }
      }
    }
    return VfsUtilCore.toVirtualFileArray(roots);
  }
}
