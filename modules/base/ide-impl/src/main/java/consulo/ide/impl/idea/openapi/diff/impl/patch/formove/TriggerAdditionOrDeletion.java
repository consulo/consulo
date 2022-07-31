/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.patch.formove;

import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.*;
import consulo.ide.impl.idea.openapi.vcs.changes.SortByVcsRoots;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.FilePathByPathComparator;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.util.collection.MultiMap;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.util.*;

public class TriggerAdditionOrDeletion {
  private final Collection<FilePath> myExisting;
  private final Collection<FilePath> myDeleted;
  private final Set<FilePath> myAffected;
  private final Project myProject;
  private final boolean mySilentAddDelete;
  private final ProjectLevelVcsManager myVcsManager;
  private final AbstractVcsHelper myVcsHelper;
  private static final Logger LOG = Logger.getInstance(TriggerAdditionOrDeletion.class);
  private final VcsFileListenerContextHelper myVcsFileListenerContextHelper;

  private MultiMap<VcsRoot, FilePath> myPreparedAddition;
  private MultiMap<VcsRoot, FilePath> myPreparedDeletion;

  public TriggerAdditionOrDeletion(final Project project) {
    myProject = project;
    mySilentAddDelete = Registry.is("vcs.add.remove.silent");
    myExisting = new HashSet<>();
    myDeleted = new HashSet<>();
    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    myVcsHelper = AbstractVcsHelper.getInstance(myProject);
    myAffected = new HashSet<>();
    myVcsFileListenerContextHelper = VcsFileListenerContextHelper.getInstance(myProject);
  }

  public void addExisting(final Collection<FilePath> files) {
    myExisting.addAll(files);
  }

  public void addDeleted(final Collection<FilePath> files) {
    myDeleted.addAll(files);
  }

  public void prepare() {
    if (myExisting.isEmpty() && myDeleted.isEmpty()) return;

    final SortByVcsRoots<FilePath> sortByVcsRoots = new SortByVcsRoots<>(myProject, new Convertor.IntoSelf<>());

    if (! myExisting.isEmpty()) {
      processAddition(sortByVcsRoots);
    }
    if (! myDeleted.isEmpty()) {
      processDeletion(sortByVcsRoots);
    }
  }

  public void processIt() {
    if (myPreparedDeletion != null) {
      for (Map.Entry<VcsRoot, Collection<FilePath>> entry : myPreparedDeletion.entrySet()) {
        final VcsRoot vcsRoot = entry.getKey();
        final AbstractVcs vcs = ObjectUtils.assertNotNull(vcsRoot.getVcs());
        final CheckinEnvironment localChangesProvider = vcs.getCheckinEnvironment();
        if (localChangesProvider == null) continue;
        final Collection<FilePath> filePaths = entry.getValue();
        if (vcs.fileListenerIsSynchronous()) {
          myAffected.addAll(filePaths);
          continue;
        }
        askUserIfNeeded(vcsRoot.getVcs(), (List<FilePath>)filePaths, VcsConfiguration.StandardConfirmation.REMOVE);
        myAffected.addAll(filePaths);
        localChangesProvider.scheduleMissingFileForDeletion((List<FilePath>)filePaths);
      }
    }
    if (myPreparedAddition != null) {
      final List<FilePath> incorrectFilePath = new ArrayList<>();
      for (Map.Entry<VcsRoot, Collection<FilePath>> entry : myPreparedAddition.entrySet()) {
        final VcsRoot vcsRoot = entry.getKey();
        final AbstractVcs vcs = ObjectUtils.assertNotNull(vcsRoot.getVcs());
        final CheckinEnvironment localChangesProvider = vcs.getCheckinEnvironment();
        if (localChangesProvider == null) continue;
        final Collection<FilePath> filePaths = entry.getValue();
        if (vcs.fileListenerIsSynchronous()) {
          myAffected.addAll(filePaths);
          continue;
        }
        askUserIfNeeded(vcsRoot.getVcs(), (List<FilePath>)filePaths, VcsConfiguration.StandardConfirmation.ADD);
        myAffected.addAll(filePaths);
        final List<VirtualFile> virtualFiles = new ArrayList<>();
        ContainerUtil.process(filePaths, new Processor<FilePath>() {
          @Override
          public boolean process(FilePath path) {
            VirtualFile vf = path.getVirtualFile();
            if (vf == null) {
              incorrectFilePath.add(path);
            }
            else {
              virtualFiles.add(vf);
            }
            return true;
          }
        });
        //virtual files collection shouldn't contain 'null' vf
        localChangesProvider.scheduleUnversionedFilesForAddition(virtualFiles);
      }
      //if some errors occurred  -> notify
      if (!incorrectFilePath.isEmpty()) {
        notifyAndLogFiles("Apply new files error", incorrectFilePath);
      }
    }
  }

  private void notifyAndLogFiles(@Nonnull String topic, @Nonnull List<FilePath> incorrectFilePath) {
    String message = "The following " + StringUtil.pluralize("file", incorrectFilePath.size()) + " may be processed incorrectly by VCS.\n" +
                     "Please check it manually: " + incorrectFilePath;
    LOG.warn(message);
    VcsNotifier.getInstance(myProject).notifyImportantWarning(topic, message);
  }

  public Set<FilePath> getAffected() {
    return myAffected;
  }

  private void processDeletion(SortByVcsRoots<FilePath> sortByVcsRoots) {
    final MultiMap<VcsRoot, FilePath> map = sortByVcsRoots.sort(myDeleted);
    myPreparedDeletion = new MultiMap<>();
    for (VcsRoot vcsRoot : map.keySet()) {
      if (vcsRoot != null && vcsRoot.getVcs() != null) {
        final CheckinEnvironment localChangesProvider = vcsRoot.getVcs().getCheckinEnvironment();
        if (localChangesProvider == null) continue;
        final boolean takeDirs = vcsRoot.getVcs().areDirectoriesVersionedItems();

        final Collection<FilePath> files = map.get(vcsRoot);
        final List<FilePath> toBeDeleted = new LinkedList<>();
        for (FilePath file : files) {
          final FilePath parent = file.getParentPath();
          if ((takeDirs || (! file.isDirectory())) && parent != null && parent.getIOFile().exists()) {
            toBeDeleted.add(file);
          }
        }
        if (toBeDeleted.isEmpty()) return;
        if (! vcsRoot.getVcs().fileListenerIsSynchronous()) {
          for (FilePath filePath : toBeDeleted) {
            myVcsFileListenerContextHelper.ignoreDeleted(filePath);
          }
        }
        myPreparedDeletion.put(vcsRoot, toBeDeleted);
      }
    }
  }

  private void processAddition(SortByVcsRoots<FilePath> sortByVcsRoots) {
    final MultiMap<VcsRoot, FilePath> map = sortByVcsRoots.sort(myExisting);
    myPreparedAddition = new MultiMap<>();
    for (VcsRoot vcsRoot : map.keySet()) {
      if (vcsRoot != null && vcsRoot.getVcs() != null) {
        final CheckinEnvironment localChangesProvider = vcsRoot.getVcs().getCheckinEnvironment();
        if (localChangesProvider == null) continue;
        final boolean takeDirs = vcsRoot.getVcs().areDirectoriesVersionedItems();

        final Collection<FilePath> files = map.get(vcsRoot);
        final List<FilePath> toBeAdded;
        if (takeDirs) {
          final RecursiveCheckAdder adder = new RecursiveCheckAdder(vcsRoot.getPath());
          for (FilePath file : files) {
            adder.process(file);
          }
          toBeAdded = adder.getToBeAdded();
        } else {
          toBeAdded = new LinkedList<>();
          for (FilePath file : files) {
            if (! file.isDirectory()) {
              toBeAdded.add(file);
            }
          }
        }
        if (toBeAdded.isEmpty()) {
          return;
        }
        Collections.sort(toBeAdded, FilePathByPathComparator.getInstance());
        if (! vcsRoot.getVcs().fileListenerIsSynchronous()) {
          for (FilePath filePath : toBeAdded) {
            myVcsFileListenerContextHelper.ignoreAdded(filePath.getVirtualFile());
          }
        }
        myPreparedAddition.put(vcsRoot, toBeAdded);
      }
    }
  }

  private void askUserIfNeeded(final AbstractVcs vcs, @Nonnull  final List<FilePath> filePaths, @Nonnull VcsConfiguration.StandardConfirmation type) {
    if (mySilentAddDelete) return;
    final VcsShowConfirmationOption confirmationOption = myVcsManager.getStandardConfirmation(type, vcs);
    if (VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY.equals(confirmationOption.getValue())) {
      filePaths.clear();
    }
    else if (VcsShowConfirmationOption.Value.SHOW_CONFIRMATION.equals(confirmationOption.getValue())) {
      String operation = type == VcsConfiguration.StandardConfirmation.ADD ? "addition" : "deletion";
      String preposition = type == VcsConfiguration.StandardConfirmation.ADD ? " to " : " from ";
      final Collection<FilePath> files = myVcsHelper.selectFilePathsToProcess(filePaths, "Select files to " +
                                                                                         StringUtil.decapitalize(type.getId()) +
                                                                                         preposition +
                                                                                         vcs.getDisplayName(), null,
                                                                              "Schedule for " + operation,
                                                                              "Do you want to schedule the following file for " +
                                                                              operation +
                                                                              preposition +
                                                                              vcs.getDisplayName() +
                                                                              "\n{0}", confirmationOption);
      if (files == null) {
        filePaths.clear();
      }
      else {
        filePaths.retainAll(files);
      }
    }
  }

  private static class RecursiveCheckAdder {
    private final Set<FilePath> myToBeAdded;
    private final VirtualFile myRoot;

    private RecursiveCheckAdder(final VirtualFile root) {
      myRoot = root;
      myToBeAdded = new HashSet<>();
    }

    public void process(final FilePath path) {
      FilePath current = path;
      while (current != null) {
        VirtualFile vf = current.getVirtualFile();
        if (vf == null) {
          vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(current.getPath());
        }
        if (vf == null) {
          return;
        }
        if (!VfsUtilCore.isAncestor(myRoot, vf, true)) return;

        myToBeAdded.add(current);
        current = current.getParentPath();
      }
    }

    public List<FilePath> getToBeAdded() {
      return new ArrayList<>(myToBeAdded);
    }
  }
}
