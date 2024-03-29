// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileChooser.tree;

import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.tree.TreeVisitor;
import jakarta.annotation.Nonnull;

import static consulo.ide.impl.idea.openapi.vfs.VfsUtilCore.isAncestor;

public class FileNodeVisitor extends TreeVisitor.ByComponent<VirtualFile, VirtualFile> {

  public FileNodeVisitor(@Nonnull VirtualFile file) {
    super(file, object -> {
      FileNode node = object instanceof FileNode ? (FileNode)object : null;
      return node == null ? null : node.getFile();
    });
  }

  @Nonnull
  @Override
  protected Action visit(VirtualFile file) {
    return file == null ? Action.CONTINUE : super.visit(file);
  }

  @Override
  protected boolean contains(@Nonnull VirtualFile pathFile, @Nonnull VirtualFile thisFile) {
    return isAncestor(pathFile, thisFile, true);
  }
}
