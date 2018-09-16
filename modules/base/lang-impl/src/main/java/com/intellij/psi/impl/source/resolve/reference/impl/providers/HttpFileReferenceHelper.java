package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

final class HttpFileReferenceHelper extends FileReferenceHelper {
  @Nullable
  @Override
  public PsiFileSystemItem findRoot(Project project, @Nonnull VirtualFile file) {
    VirtualFile root = file;
    VirtualFile parent;
    while ((parent = root.getParent()) != null) {
      root = parent;
    }
    return getPsiFileSystemItem(project, root);
  }

  @Nonnull
  @Override
  public Collection<PsiFileSystemItem> getContexts(Project project, @Nonnull VirtualFile file) {
    PsiFileSystemItem item = getPsiFileSystemItem(project, file);
    return item == null ? Collections.emptyList() : Collections.singleton(item);
  }

  @Override
  public boolean isMine(Project project, @Nonnull VirtualFile file) {
    return file instanceof HttpVirtualFile;
  }
}