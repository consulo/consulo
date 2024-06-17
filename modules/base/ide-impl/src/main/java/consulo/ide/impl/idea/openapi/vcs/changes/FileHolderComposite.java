/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.VcsDirtyScope;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

public class FileHolderComposite implements FileHolder {
  public static FileHolderComposite create(Project project) {
    return new FileHolderComposite(project);
  }

  private final Project project;
  private final CompositeFilePathHolder.UnversionedFilesCompositeHolder unversionedFileHolder;
  private final CompositeFilePathHolder.IgnoredFilesCompositeHolder ignoredFileHolder;
  private final VirtualFileHolder modifiedWithoutEditingFileHolder;
  private final VirtualFileHolder lockedFileHolder;
  private final LogicallyLockedHolder logicallyLockedFileHolder;
  private final SwitchedFileHolder rootSwitchFileHolder;
  private final SwitchedFileHolder switchedFileHolder;
  private final DeletedFilesHolder deletedFileHolder;

  private final List<FileHolder> fileHolders;

  public FileHolderComposite(final Project project) {
    this.project = project;
    unversionedFileHolder = new CompositeFilePathHolder.UnversionedFilesCompositeHolder(project);
    ignoredFileHolder = new CompositeFilePathHolder.IgnoredFilesCompositeHolder(project);
    modifiedWithoutEditingFileHolder = new VirtualFileHolder(project);
    lockedFileHolder = new VirtualFileHolder(project);
    logicallyLockedFileHolder = new LogicallyLockedHolder(project);
    rootSwitchFileHolder = new SwitchedFileHolder(project);
    switchedFileHolder = new SwitchedFileHolder(project);
    deletedFileHolder = new DeletedFilesHolder();

    this.fileHolders =
      List.of(unversionedFileHolder, ignoredFileHolder, modifiedWithoutEditingFileHolder, lockedFileHolder, logicallyLockedFileHolder,
              rootSwitchFileHolder, switchedFileHolder, deletedFileHolder);
  }

  public FileHolderComposite(Project project,
                             CompositeFilePathHolder.UnversionedFilesCompositeHolder unversionedFileHolder,
                             CompositeFilePathHolder.IgnoredFilesCompositeHolder ignoredFileHolder,
                             VirtualFileHolder modifiedWithoutEditingFileHolder,
                             VirtualFileHolder lockedFileHolder,
                             LogicallyLockedHolder logicallyLockedFileHolder,
                             SwitchedFileHolder rootSwitchFileHolder,
                             SwitchedFileHolder switchedFileHolder,
                             DeletedFilesHolder deletedFileHolder) {
    this.project = project;
    this.unversionedFileHolder = unversionedFileHolder;
    this.ignoredFileHolder = ignoredFileHolder;
    this.modifiedWithoutEditingFileHolder = modifiedWithoutEditingFileHolder;
    this.lockedFileHolder = lockedFileHolder;
    this.logicallyLockedFileHolder = logicallyLockedFileHolder;
    this.rootSwitchFileHolder = rootSwitchFileHolder;
    this.switchedFileHolder = switchedFileHolder;
    this.deletedFileHolder = deletedFileHolder;

    this.fileHolders =
      List.of(unversionedFileHolder, ignoredFileHolder, modifiedWithoutEditingFileHolder, lockedFileHolder, logicallyLockedFileHolder,
              rootSwitchFileHolder, switchedFileHolder, deletedFileHolder);
  }

  public Project getProject() {
    return project;
  }

  public CompositeFilePathHolder.UnversionedFilesCompositeHolder getUnversionedFileHolder() {
    return unversionedFileHolder;
  }

  public CompositeFilePathHolder.IgnoredFilesCompositeHolder getIgnoredFileHolder() {
    return ignoredFileHolder;
  }

  public VirtualFileHolder getModifiedWithoutEditingFileHolder() {
    return modifiedWithoutEditingFileHolder;
  }

  public VirtualFileHolder getLockedFileHolder() {
    return lockedFileHolder;
  }

  public LogicallyLockedHolder getLogicallyLockedFileHolder() {
    return logicallyLockedFileHolder;
  }

  public SwitchedFileHolder getRootSwitchFileHolder() {
    return rootSwitchFileHolder;
  }

  public SwitchedFileHolder getSwitchedFileHolder() {
    return switchedFileHolder;
  }

  public DeletedFilesHolder getDeletedFileHolder() {
    return deletedFileHolder;
  }

  public List<FileHolder> getFileHolders() {
    return fileHolders;
  }

  @Override
  public void cleanAll() {
    for (FileHolder holder : getFileHolders()) {
      holder.cleanAll();
    }
  }

  @Override
  public void cleanUnderScope(@Nonnull final VcsDirtyScope scope) {
    for (FileHolder fileHolder : getFileHolders()) {
      fileHolder.cleanUnderScope(scope);
    }
  }

  @Override
  public FileHolderComposite copy() {
    return new FileHolderComposite(project,
                                   unversionedFileHolder.copy(),
                                   ignoredFileHolder.copy(),
                                   modifiedWithoutEditingFileHolder.copy(),
                                   lockedFileHolder.copy(),
                                   logicallyLockedFileHolder.copy(),
                                   rootSwitchFileHolder.copy(),
                                   switchedFileHolder.copy(),
                                   deletedFileHolder.copy());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final FileHolderComposite another = (FileHolderComposite)o;
    return Objects.equals(getFileHolders(), another.getFileHolders());
  }

  @Override
  public int hashCode() {
    return getFileHolders().hashCode();
  }

  @Override
  public void notifyVcsStarted(AbstractVcs vcs) {
    for (FileHolder fileHolder : fileHolders) {
      fileHolder.notifyVcsStarted(vcs);
    }
  }
}
