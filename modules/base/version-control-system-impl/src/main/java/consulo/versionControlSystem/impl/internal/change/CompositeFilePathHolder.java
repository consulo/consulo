/*
 * Copyright 2000-2025 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.root.VcsRoot;

import java.util.*;

public abstract class CompositeFilePathHolder implements FileHolder {
  protected final Project myProject;

  private final Map<AbstractVcs, FilePathHolder> myMap = new HashMap<>();

  public CompositeFilePathHolder(Project project) {
    myProject = project;
  }

  @Override
  public void cleanAll() {
    myMap.values().forEach(FilePathHolder::cleanAll);
    myMap.clear();
  }

  @Override
  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    AbstractVcs vcs = scope.getVcs();
    FilePathHolder holder = myMap.get(vcs);
    if (holder != null) {
      holder.cleanAndAdjustScope(scope);
    }
  }

  protected void copyFrom(CompositeFilePathHolder holder) {
    for (Map.Entry<AbstractVcs, FilePathHolder> entry : holder.myMap.entrySet()) {
      myMap.put(entry.getKey(), (FilePathHolder) entry.getValue().copy());
    }
  }

  public void addFile(AbstractVcs vcs, FilePath file) {
    FilePathHolder holder = myMap.get(vcs);
    if (holder != null) {
      holder.addFile(file);
    }
  }

  public boolean isInUpdatingMode() {
    return myMap.values().stream()
      .anyMatch(holder -> holder instanceof VcsManagedFilesHolder && ((VcsManagedFilesHolder) holder).isInUpdatingMode());
  }

  public boolean containsFile(FilePath file, VcsRoot vcsRoot) {
    FilePathHolder holder = myMap.get(vcsRoot.getVcs());
    return holder != null && holder.containsFile(file, vcsRoot.getPath());
  }

  public Collection<FilePath> getFiles() {
    HashSet<FilePath> result = new HashSet<>();
    for (FilePathHolder fileHolder : myMap.values()) {
      result.addAll(fileHolder.values());
    }
    return result;
  }

  @Override
  public void notifyVcsStarted(AbstractVcs vcs) {
    if (!myMap.containsKey(vcs)) {
      myMap.put(vcs, createHolderForVcs(myProject, vcs));
    }

    for (FileHolder fileHolder : myMap.values()) {
      fileHolder.notifyVcsStarted(vcs);
    }
  }

  protected abstract FilePathHolder createHolderForVcs(Project project, AbstractVcs vcs);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompositeFilePathHolder holder = (CompositeFilePathHolder) o;
    return Objects.equals(myMap, holder.myMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myMap);
  }

  public static class UnversionedFilesCompositeHolder extends CompositeFilePathHolder {
    public UnversionedFilesCompositeHolder(Project project) {
      super(project);
    }

    @Override
    protected FilePathHolder createHolderForVcs(Project project, AbstractVcs vcs) {
      VcsManagedUnversionedFilesHolderProvider provider =
        project.getExtensionPoint(VcsManagedUnversionedFilesHolderProvider.class)
          .findFirstSafe(ep -> ep.getVcs().equals(vcs));
      if (provider != null) {
        return provider.createHolder();
      }
      return new RecursiveFilePathHolderImpl(project, FileHolder.HolderType.UNVERSIONED);
    }

    @Override
    public UnversionedFilesCompositeHolder copy() {
      UnversionedFilesCompositeHolder result = new UnversionedFilesCompositeHolder(myProject);
      result.copyFrom(this);
      return result;
    }

    @Override
    public HolderType getType() {
      return HolderType.UNVERSIONED;
    }
  }

  public static class ResolvedFilesCompositeHolder extends CompositeFilePathHolder {
    public ResolvedFilesCompositeHolder(Project project) {
      super(project);
    }

    @Override
    protected FilePathHolder createHolderForVcs(Project project, AbstractVcs vcs) {
      VcsManagedResolvedConflictsFilesHolderProvider provider =
        project.getExtensionPoint(VcsManagedResolvedConflictsFilesHolderProvider.class)
          .findFirstSafe(ep -> ep.getVcs().equals(vcs));
      if (provider != null) {
        return provider.createHolder();
      }
      return new RecursiveFilePathHolderImpl(project, FileHolder.HolderType.RESOLVED);
    }

    @Override
    public ResolvedFilesCompositeHolder copy() {
      ResolvedFilesCompositeHolder result = new ResolvedFilesCompositeHolder(myProject);
      result.copyFrom(this);
      return result;
    }

    @Override
    public HolderType getType() {
      return HolderType.RESOLVED;
    }
  }

  public static class IgnoredFilesCompositeHolder extends CompositeFilePathHolder {
    public IgnoredFilesCompositeHolder(Project project) {
      super(project);
    }

    @Override
    protected FilePathHolder createHolderForVcs(Project project, AbstractVcs vcs) {
      VcsManagedIgnoredFilesHolderProvider provider =
        project.getExtensionPoint(VcsManagedIgnoredFilesHolderProvider.class)
          .findFirstSafe(ep -> ep.getVcs().equals(vcs));
      if (provider != null) {
        return provider.createHolder();
      }
      return new RecursiveFilePathHolderImpl(project, FileHolder.HolderType.IGNORED);
    }

    @Override
    public IgnoredFilesCompositeHolder copy() {
      IgnoredFilesCompositeHolder result = new IgnoredFilesCompositeHolder(myProject);
      result.copyFrom(this);
      return result;
    }

    @Override
    public HolderType getType() {
      return HolderType.IGNORED;
    }
  }
}
