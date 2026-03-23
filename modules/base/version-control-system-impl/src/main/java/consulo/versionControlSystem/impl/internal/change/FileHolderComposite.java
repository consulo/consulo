/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors.
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
import consulo.versionControlSystem.change.DeletedFilesHolder;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FileHolderComposite implements FileHolder {
  private final Project myProject;

  private final CompositeFilePathHolder.UnversionedFilesCompositeHolder myUnversionedFileHolder;
  private final CompositeFilePathHolder.IgnoredFilesCompositeHolder myIgnoredFileHolder;
  private final VirtualFileHolder myModifiedWithoutEditingFileHolder;
  private final VirtualFileHolder myLockedFileHolder;
  private final LogicallyLockedHolder myLogicallyLockedFileHolder;
  private final SwitchedFileHolder myRootSwitchFileHolder;
  private final SwitchedFileHolder mySwitchedFileHolder;
  private final DeletedFilesHolder myDeletedFileHolder;
  private final CompositeFilePathHolder.ResolvedFilesCompositeHolder myResolvedMergeFilesHolder;

  public FileHolderComposite(Project project) {
    this(project,
         new CompositeFilePathHolder.UnversionedFilesCompositeHolder(project),
         new CompositeFilePathHolder.IgnoredFilesCompositeHolder(project),
         new VirtualFileHolder(project, FileHolder.HolderType.MODIFIED_WITHOUT_EDITING),
         new VirtualFileHolder(project, FileHolder.HolderType.LOCKED),
         new LogicallyLockedHolder(project),
         new SwitchedFileHolder(project, FileHolder.HolderType.ROOT_SWITCH),
         new SwitchedFileHolder(project, FileHolder.HolderType.SWITCHED),
         new DeletedFilesHolder(),
         new CompositeFilePathHolder.ResolvedFilesCompositeHolder(project));
  }

  private FileHolderComposite(Project project,
                               CompositeFilePathHolder.UnversionedFilesCompositeHolder unversioned,
                               CompositeFilePathHolder.IgnoredFilesCompositeHolder ignored,
                               VirtualFileHolder modifiedWithoutEditing,
                               VirtualFileHolder locked,
                               LogicallyLockedHolder logicallyLocked,
                               SwitchedFileHolder rootSwitch,
                               SwitchedFileHolder switched,
                               DeletedFilesHolder deleted,
                               CompositeFilePathHolder.ResolvedFilesCompositeHolder resolved) {
    myProject = project;
    myUnversionedFileHolder = unversioned;
    myIgnoredFileHolder = ignored;
    myModifiedWithoutEditingFileHolder = modifiedWithoutEditing;
    myLockedFileHolder = locked;
    myLogicallyLockedFileHolder = logicallyLocked;
    myRootSwitchFileHolder = rootSwitch;
    mySwitchedFileHolder = switched;
    myDeletedFileHolder = deleted;
    myResolvedMergeFilesHolder = resolved;
  }

  private List<FileHolder> fileHolders() {
    return Arrays.asList(myUnversionedFileHolder, myIgnoredFileHolder, myModifiedWithoutEditingFileHolder,
                         myLockedFileHolder, myLogicallyLockedFileHolder,
                         myRootSwitchFileHolder, mySwitchedFileHolder,
                         myDeletedFileHolder, myResolvedMergeFilesHolder);
  }

  @Override
  public void cleanAll() {
    fileHolders().forEach(FileHolder::cleanAll);
  }

  @Override
  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    fileHolders().forEach(h -> h.cleanAndAdjustScope(scope));
  }

  @Override
  public FileHolderComposite copy() {
    return new FileHolderComposite(myProject,
                                   (CompositeFilePathHolder.UnversionedFilesCompositeHolder) myUnversionedFileHolder.copy(),
                                   (CompositeFilePathHolder.IgnoredFilesCompositeHolder) myIgnoredFileHolder.copy(),
                                   (VirtualFileHolder) myModifiedWithoutEditingFileHolder.copy(),
                                   (VirtualFileHolder) myLockedFileHolder.copy(),
                                   (LogicallyLockedHolder) myLogicallyLockedFileHolder.copy(),
                                   (SwitchedFileHolder) myRootSwitchFileHolder.copy(),
                                   (SwitchedFileHolder) mySwitchedFileHolder.copy(),
                                   myDeletedFileHolder.copy(),
                                   (CompositeFilePathHolder.ResolvedFilesCompositeHolder) myResolvedMergeFilesHolder.copy());
  }

  @Override
  public void notifyVcsStarted(AbstractVcs vcs) {
    fileHolders().forEach(h -> h.notifyVcsStarted(vcs));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FileHolderComposite another = (FileHolderComposite) o;
    return Objects.equals(fileHolders(), another.fileHolders());
  }

  @Override
  public int hashCode() {
    return fileHolders().hashCode();
  }

  @Override
  public HolderType getType() {
    throw new UnsupportedOperationException();
  }

  // Named accessors matching JetBrains FileHolderComposite fields

  public CompositeFilePathHolder.UnversionedFilesCompositeHolder getUnversionedFileHolder() {
    return myUnversionedFileHolder;
  }

  public CompositeFilePathHolder.IgnoredFilesCompositeHolder getIgnoredFileHolder() {
    return myIgnoredFileHolder;
  }

  public VirtualFileHolder getModifiedWithoutEditingFileHolder() {
    return myModifiedWithoutEditingFileHolder;
  }

  public VirtualFileHolder getLockedFileHolder() {
    return myLockedFileHolder;
  }

  public LogicallyLockedHolder getLogicallyLockedFileHolder() {
    return myLogicallyLockedFileHolder;
  }

  public SwitchedFileHolder getRootSwitchFileHolder() {
    return myRootSwitchFileHolder;
  }

  public SwitchedFileHolder getSwitchedFileHolder() {
    return mySwitchedFileHolder;
  }

  public DeletedFilesHolder getDeletedFileHolder() {
    return myDeletedFileHolder;
  }

  public CompositeFilePathHolder.ResolvedFilesCompositeHolder getResolvedMergeFilesHolder() {
    return myResolvedMergeFilesHolder;
  }
}
