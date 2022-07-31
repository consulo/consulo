package consulo.versionControlSystem.distributed.repository;

import consulo.application.AccessRule;
import consulo.application.util.function.ThrowableComputable;
import consulo.util.collection.ContainerUtil;
import consulo.vcs.AbstractVcs;
import consulo.vcs.FilePath;
import consulo.vcs.change.ChangesUtil;
import consulo.virtualFileSystem.VirtualFile;

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
  @Nullable
  public T getRepositoryForRoot(@Nullable VirtualFile root) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForRoot(root));
  }

  @Nullable
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
  @Nullable
  @Deprecated
  public T getRepositoryForFileQuick(@Nonnull VirtualFile file) {
    return validateAndGetRepository(myGlobalRepositoryManager.getRepositoryForFileQuick(file));
  }

  @Override
  @Nullable
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
  public void updateRepository(@Nullable VirtualFile root) {
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

  @Nullable
  private T validateAndGetRepository(@Nullable Repository repository) {
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
