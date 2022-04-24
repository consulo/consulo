package consulo.ide.impl.psi.impl.source.resolve.reference.impl.providers;

import consulo.language.psi.path.FileReferenceHelper;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.language.psi.PsiFileSystemItem;
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