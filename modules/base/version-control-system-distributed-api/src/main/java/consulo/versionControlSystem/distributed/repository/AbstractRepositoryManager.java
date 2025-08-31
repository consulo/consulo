package consulo.versionControlSystem.distributed.repository;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractRepositoryManager<T extends Repository> implements RepositoryManager<T> {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VcsKey myVcsKey;
    @Nonnull
    private final VcsRepositoryManager myGlobalRepositoryManager;

    protected AbstractRepositoryManager(@Nonnull Project project,
                                        @Nonnull VcsRepositoryManager globalRepositoryManager,
                                        @Nonnull VcsKey vcsKey) {
        myProject = project;
        myGlobalRepositoryManager = globalRepositoryManager;
        myVcsKey = vcsKey;
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

    @Nullable
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
    @SuppressWarnings("unchecked")
    private T validateAndGetRepository(@Nullable Repository repository) {
        if (repository == null ||
            !myVcsKey.equals(repository.getVcs().getKeyInstanceMethod()) ||
            !repository.getRoot().isValid()) {
            return null;
        }

        return (T) repository;
    }

    @Override
    @Nonnull
    public AbstractVcs getVcs() {
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).findVcsByName(myVcsKey.getName());
        return Objects.requireNonNull(vcs);
    }
}
