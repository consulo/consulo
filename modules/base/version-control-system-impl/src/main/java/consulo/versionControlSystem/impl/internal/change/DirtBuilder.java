package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.VcsDirtyScopeBuilder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DirtBuilder {
  private Map<AbstractVcs, VcsDirtyScopeBuilder> scopesByVcs = new HashMap<>();

  private boolean isEverythingDirty = false;

  public DirtBuilder() {
  }

  public final boolean isEverythingDirty() {
    return isEverythingDirty;
  }

  public void setEverythingDirty(boolean everythingDirty) {
    isEverythingDirty = everythingDirty;
  }

  public final boolean isEmpty() {
    return !isEverythingDirty() && scopesByVcs.isEmpty();
  }

  public final void markEverythingDirty() {
    setEverythingDirty(true);
    scopesByVcs.clear();
  }

  public final boolean addDirtyFiles(@Nonnull VcsRoot vcsRoot,
                                     @Nonnull Collection<? extends FilePath> files, @Nonnull Collection<? extends FilePath> dirs) {
    if (isEverythingDirty()) {
      return true;
    }
    AbstractVcs vcs = vcsRoot.getVcs();
    VirtualFile root = vcsRoot.getPath();
    if (vcs != null) {
      VcsDirtyScopeBuilder scope = scopesByVcs.computeIfAbsent(vcs, this::createDirtyScope);
      for (FilePath filePath : files) {
        scope.addDirtyPathFast(root, filePath, false);
      }
      for (FilePath filePath : dirs) {
        scope.addDirtyPathFast(root, filePath, true);
      }
    }
    return !scopesByVcs.isEmpty();
  }

  @Nonnull
  public final List<VcsModifiableDirtyScope> buildScopes(@Nonnull Project project) {
    Collection<VcsDirtyScopeBuilder> scopes;
    if (isEverythingDirty()) {
      Map<AbstractVcs, VcsDirtyScopeBuilder> allScopes = new HashMap<>();
      for (VcsRoot root : ProjectLevelVcsManager.getInstance(project).getAllVcsRoots()) {
        AbstractVcs vcs = root.getVcs();
        VirtualFile path = root.getPath();
        if (vcs != null) {
          VcsDirtyScopeBuilder scope = allScopes.computeIfAbsent(vcs, this::createDirtyScope);
          scope.markEverythingDirty();
          scope.addDirtyPathFast(path, VcsUtil.getFilePath(path), true);
        }
      }
      scopes = allScopes.values();
    }
    else {
      scopes = scopesByVcs.values();
    }
    return ContainerUtil.map(scopes, VcsDirtyScopeBuilder::pack);
  }

  public final boolean isFileDirty(@Nonnull FilePath filePath) {
    return isEverythingDirty() || ContainerUtil.any(scopesByVcs.values(), (it) -> it.belongsTo(filePath));
  }

  private VcsDirtyScopeBuilder createDirtyScope(AbstractVcs vcs) {
    return vcs.createDirtyScope() == null ? new VcsDirtyScopeImpl(vcs) : vcs.createDirtyScope();
  }
}
