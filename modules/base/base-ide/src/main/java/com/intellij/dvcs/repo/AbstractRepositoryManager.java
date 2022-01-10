package com.intellij.dvcs.repo;

import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import consulo.application.AccessRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public abstract class AbstractRepositoryManager<T extends Repository>
        implements RepositoryManager<T> {

  @Nonnull
  private final AbstractVcs myVcs;
  @Nonnull
  private final String myRepoDirName;
  @Nonnull
  private final VcsRepositoryManager myGlobalRepositoryManager;

  protected AbstractRepositoryManager(@Nonnull VcsRepositoryManager globalRepositoryManager,
                                      @Nonnull AbstractVcs vcs,
                                      @Nonnull String repoDirName) {
    myGlobalRepositoryManager = globalRepositoryManager;
    myVcs = vcs;
    myRepoDirName = repoDirName;
  }

  @Override
  @javax.annotation.Nullable
  public T getRepositoryForRoot(@javax.annotation.Nullable VirtualFile root) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForRoot(root));
  }

  @javax.annotation.Nullable
  public T getRepositoryForRootQuick(@Nullable VirtualFile root) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForRootQuick(root));
  }

  @Override
  public void addExternalRepository(@Nonnull VirtualFile root, @Nonnull T repository) {
    myGlobalRepositoryManager.addExternalRepository(root, repository);
  }

  @Override
  public void removeExternalRepository(@Nonnull VirtualFile root) {
    myGlobalRepositoryManager.removeExternalRepository(root);
  }

  @Override
  public boolean isExternal(@Nonnull T repository) {
    return myGlobalRepositoryManager.isExternal(repository);
  }

  @Override
  @Nullable
  public T getRepositoryForFile(@Nonnull VirtualFile file) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForFile(file));
  }

  /**
   * @Deprecated to delete in 2017.X
   */
  @javax.annotation.Nullable
  @Deprecated
  public T getRepositoryForFileQuick(@Nonnull VirtualFile file) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForFileQuick(file));
  }

  @Override
  @javax.annotation.Nullable
  public T getRepositoryForFile(@Nonnull FilePath file) {
    VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
    return vFile != null ? getRepositoryForFile(vFile) : null;
  }

  @Nonnull
  protected List<T> getRepositories(Class<T> type) {
    return ContainerUtil.findAll(myGlobalRepositoryManager.getRepositories(), type);
  }

  @Nonnull
  @Override
  public abstract List<T> getRepositories();

  @Override
  public boolean moreThanOneRoot() {
    return getRepositories().size() > 1;
  }

  @Override
  public void updateRepository(@javax.annotation.Nullable VirtualFile root) {
    T repo = getRepositoryForRoot(root);
    if (repo != null) {
      repo.update();
    }
  }

  @Override
  public void updateAllRepositories() {
    ContainerUtil.process(getRepositories(), repo -> {
      repo.update();
      return true;
    });
  }

  @javax.annotation.Nullable
  private T validateAndGetRepository(@javax.annotation.Nullable Repository repository) {
    if (repository == null || !myVcs.equals(repository.getVcs())) return null;
    ThrowableComputable<T,RuntimeException> action = () -> {
      VirtualFile root = repository.getRoot();
      if (root.isValid()) {
        VirtualFile vcsDir = root.findChild(myRepoDirName);
        //noinspection unchecked
        return vcsDir != null && vcsDir.exists() ? (T)repository : null;
      }
      return null;
    };
    return AccessRule.read(action);
  }

  @Override
  @Nonnull
  public AbstractVcs getVcs() {
    return myVcs;
  }

}
