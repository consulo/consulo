/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import consulo.versionControlSystem.impl.internal.change.FileHolderComposite;
import consulo.versionControlSystem.impl.internal.change.LogicallyLockedHolder;
import consulo.versionControlSystem.impl.internal.change.SwitchedFileHolder;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

class UpdatingChangeListBuilder implements ChangelistBuilder {
  private static final Logger LOG = Logger.getInstance(UpdatingChangeListBuilder.class);
  private final ChangeListWorker myChangeListWorker;
  private final FileHolderComposite myComposite;
  // todo +-
  private final Supplier<Boolean> myDisposedGetter;
  private VcsDirtyScope myScope;
  private FoldersCutDownWorker myFoldersCutDownWorker;
  private final ChangeListManager myСhangeListManager;
  private final ProjectLevelVcsManager myVcsManager;
  private final ChangeListManagerGate myGate;
  private Supplier<JComponent> myAdditionalInfo;

  UpdatingChangeListBuilder(final ChangeListWorker changeListWorker,
                            final FileHolderComposite composite,
                            final Supplier<Boolean> disposedGetter,
                            final ChangeListManager сhangeListManager,
                            final ChangeListManagerGate gate) {
    myChangeListWorker = changeListWorker;
    myComposite = composite;
    myDisposedGetter = disposedGetter;
    myСhangeListManager = сhangeListManager;
    myGate = gate;
    myVcsManager = ProjectLevelVcsManager.getInstance(changeListWorker.getProject());
  }

  private void checkIfDisposed() {
    if (myDisposedGetter.get()) throw new ProcessCanceledException();
  }

  public void setCurrent(final VcsDirtyScope scope, final FoldersCutDownWorker foldersWorker) {
    myScope = scope;
    myFoldersCutDownWorker = foldersWorker;
  }

  @Override
  public void processChange(final Change change, VcsKey vcsKey) {
    processChangeInList(change, (ChangeList)null, vcsKey);
  }

  @Override
  public void processChangeInList(final Change change, @Nullable final ChangeList changeList, final VcsKey vcsKey) {
    checkIfDisposed();

    LOG.debug("[processChangeInList-1] entering, cl name: " + ((changeList == null) ? null : changeList.getName()) +
              " change: " + ChangesUtil.getFilePath(change).getPath());
    final String fileName = ChangesUtil.getFilePath(change).getName();
    if (FileTypeManager.getInstance().isFileIgnored(fileName)) {
      LOG.debug("[processChangeInList-1] file type ignored");
      return;
    }

    if (ChangeListManagerImpl.isUnder(change, myScope)) {
      if (changeList != null) {
        LOG.debug("[processChangeInList-1] to add change to cl");
        myChangeListWorker.addChangeToList(changeList.getName(), change, vcsKey);
      }
      else {
        LOG.debug("[processChangeInList-1] to add to corresponding list");
        myChangeListWorker.addChangeToCorrespondingList(change, vcsKey);
      }
    }
    else {
      LOG.debug("[processChangeInList-1] not under scope");
    }
  }

  @Override
  public void processChangeInList(final Change change, final String changeListName, VcsKey vcsKey) {
    checkIfDisposed();

    LocalChangeList list = null;
    if (changeListName != null) {
      list = myChangeListWorker.getCopyByName(changeListName);
      if (list == null) {
        list = myGate.addChangeList(changeListName, null);
      }
    }
    processChangeInList(change, list, vcsKey);
  }

  @Override
  public void removeRegisteredChangeFor(FilePath path) {
    myChangeListWorker.removeRegisteredChangeFor(path);
  }

  private boolean isIgnoredByVcs(final VirtualFile file) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>)() -> {
      checkIfDisposed();
      return myVcsManager.isIgnored(file);
    });
  }

  @Override
  public void processUnversionedFile(final VirtualFile file) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("processUnversionedFile " + file);
    }
    if (file == null) return;
    checkIfDisposed();
    if (isIgnoredByVcs(file)) return;
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      if (myСhangeListManager.isIgnoredFile(file)) {
        myComposite.getIgnoredFileHolder().addFile(file);
      }
      else if (myComposite.getIgnoredFileHolder().containsFile(file)) {
        // does not need to add: parent dir is already added
      }
      else {
        myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED).addFile(file);
      }
      // if a file was previously marked as switched through recursion, remove it from switched list
      myChangeListWorker.removeSwitched(file);
    }
  }

  @Override
  public void processLocallyDeletedFile(final FilePath file) {
    processLocallyDeletedFile(new LocallyDeletedChange(file));
  }

  @Override
  public void processLocallyDeletedFile(LocallyDeletedChange locallyDeletedChange) {
    checkIfDisposed();
    final FilePath file = locallyDeletedChange.getPath();
    if (FileTypeManager.getInstance().isFileIgnored(file.getName())) return;
    if (myScope.belongsTo(file)) {
      myChangeListWorker.addLocallyDeleted(locallyDeletedChange);
    }
  }

  @Override
  public void processModifiedWithoutCheckout(final VirtualFile file) {
    if (file == null) return;
    checkIfDisposed();
    if (isIgnoredByVcs(file)) return;
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("processModifiedWithoutCheckout " + file);
      }
      myComposite.getVFHolder(FileHolder.HolderType.MODIFIED_WITHOUT_EDITING).addFile(file);
    }
  }

  @Override
  public void processIgnoredFile(final VirtualFile file) {
    if (file == null) return;
    checkIfDisposed();
    if (isIgnoredByVcs(file)) return;
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      ObjectUtil.assertNotNull(myComposite.getIgnoredFileHolder().getActiveVcsHolder()).addFile(file);
    }
  }

  @Override
  public void processLockedFolder(final VirtualFile file) {
    if (file == null) return;
    checkIfDisposed();
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      if (myFoldersCutDownWorker.addCurrent(file)) {
        myComposite.getVFHolder(FileHolder.HolderType.LOCKED).addFile(file);
      }
    }
  }

  @Override
  public void processLogicallyLockedFolder(VirtualFile file, LogicalLock logicalLock) {
    if (file == null) return;
    checkIfDisposed();
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      ((LogicallyLockedHolder)myComposite.get(FileHolder.HolderType.LOGICALLY_LOCKED)).add(file, logicalLock);
    }
  }

  @Override
  public void processSwitchedFile(final VirtualFile file, final String branch, final boolean recursive) {
    if (file == null) return;
    checkIfDisposed();
    if (isIgnoredByVcs(file)) return;
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      myChangeListWorker.addSwitched(file, branch, recursive);
    }
  }

  @Override
  public void processRootSwitch(VirtualFile file, String branch) {
    if (file == null) return;
    checkIfDisposed();
    if (myScope.belongsTo(VcsUtil.getFilePath(file))) {
      ((SwitchedFileHolder)myComposite.get(FileHolder.HolderType.ROOT_SWITCH)).addFile(file, branch, false);
    }
  }

  @Override
  public boolean reportChangesOutsideProject() {
    return false;
  }

  @Override
  public void reportAdditionalInfo(String text) {
    reportAdditionalInfo(ChangesViewManager.createTextStatusFactory(text, true));
  }

  @Override
  public void reportAdditionalInfo(Supplier<JComponent> infoComponent) {
    if (myAdditionalInfo == null) {
      myAdditionalInfo = infoComponent;
    }
  }

  public Supplier<JComponent> getAdditionalInfo() {
    return myAdditionalInfo;
  }
}
