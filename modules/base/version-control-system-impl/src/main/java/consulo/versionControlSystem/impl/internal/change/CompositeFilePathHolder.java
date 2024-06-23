// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.root.VcsRoot;
import jakarta.annotation.Nonnull;

import java.util.*;

public abstract class CompositeFilePathHolder implements FileHolder {
  protected final Project myProject;

  private final Map<AbstractVcs, FilePathHolder> myMap = new HashMap<>();

  public CompositeFilePathHolder(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void cleanAll() {
    myMap.values().forEach(FilePathHolder::cleanAll);
    myMap.clear();
  }

  @Override
  public void cleanUnderScope(@Nonnull VcsDirtyScope scope) {
    AbstractVcs vcs = scope.getVcs();
    FilePathHolder holder = myMap.get(vcs);
    if (holder != null) {
      holder.cleanUnderScope(scope);
    }
  }

  protected void copyFrom(@Nonnull CompositeFilePathHolder holder) {
    for (Map.Entry<AbstractVcs, FilePathHolder> entry : holder.myMap.entrySet()) {
      myMap.put(entry.getKey(), (FilePathHolder)entry.getValue().copy());
    }
  }

  public void addFile(@Nonnull AbstractVcs vcs, @Nonnull FilePath file) {
    myMap.get(vcs).addFile(file);
  }

  public boolean isInUpdatingMode() {
    return myMap.values().stream()
                .anyMatch(holder -> holder instanceof VcsManagedFilesHolder && ((VcsManagedFilesHolder)holder).isInUpdatingMode());
  }

  public boolean containsFile(@Nonnull FilePath file, @Nonnull VcsRoot vcsRoot) {
    FilePathHolder holder = myMap.get(vcsRoot.getVcs());
    return holder != null && holder.containsFile(file, vcsRoot.getPath());
  }

  @Nonnull
  public Collection<FilePath> getFiles() {
    HashSet<FilePath> result = new HashSet<>();
    for (FilePathHolder fileHolder : myMap.values()) {
      result.addAll(fileHolder.values());
    }
    return result;
  }

  @Override
  public void notifyVcsStarted(@Nonnull AbstractVcs vcs) {
    if (!myMap.containsKey(vcs)) {
      myMap.put(vcs, createHolderForVcs(myProject, vcs));
    }

    for (FileHolder fileHolder : myMap.values()) {
      fileHolder.notifyVcsStarted(vcs);
    }
  }

  @Nonnull
  protected abstract FilePathHolder createHolderForVcs(@Nonnull Project project, @Nonnull AbstractVcs vcs);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompositeFilePathHolder holder = (CompositeFilePathHolder)o;
    return Objects.equals(myMap, holder.myMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myMap);
  }

  public static class UnversionedFilesCompositeHolder extends CompositeFilePathHolder {
    public UnversionedFilesCompositeHolder(@Nonnull Project project) {
      super(project);
    }

    @Nonnull
    @Override
    protected FilePathHolder createHolderForVcs(@Nonnull Project project, @Nonnull AbstractVcs vcs) {
      VcsUnversionedFilesHolderProvider provider = VcsManagedFilesHolder.VCS_UNVERSIONED_FILES_HOLDER_EP
        .findFirstSafe(project, ep -> ep.getVcs().equals(vcs));
      if (provider != null) {
        return provider.createHolder();
      }
      else {
        return new FilePathHolderImpl(project);
      }
    }

    @Override
    public UnversionedFilesCompositeHolder copy() {
      UnversionedFilesCompositeHolder result = new UnversionedFilesCompositeHolder(myProject);
      result.copyFrom(this);
      return result;
    }
  }

  public static class IgnoredFilesCompositeHolder extends CompositeFilePathHolder {
    public IgnoredFilesCompositeHolder(@Nonnull Project project) {
      super(project);
    }

    @Nonnull
    @Override
    protected FilePathHolder createHolderForVcs(@Nonnull Project project, @Nonnull AbstractVcs vcs) {
      VcsIgnoredFilesHolderProvider provider = VcsManagedFilesHolder.VCS_IGNORED_FILES_HOLDER_EP
        .findFirstSafe(project, ep -> ep.getVcs().equals(vcs));
      if (provider != null) {
        return provider.createHolder();
      }
      else {
        return new RecursiveFilePathHolderImpl(project);
      }
    }

    @Override
    public IgnoredFilesCompositeHolder copy() {
      IgnoredFilesCompositeHolder result = new IgnoredFilesCompositeHolder(myProject);
      result.copyFrom(this);
      return result;
    }
  }
}
