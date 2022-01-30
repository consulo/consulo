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
package com.intellij.openapi.vcs.changes.shelf;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.actions.impl.GoToChangePopupBuilder;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.UnknownFileTypeDiffRequest;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.dataContext.DataContext;
import com.intellij.openapi.diff.impl.patch.*;
import com.intellij.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import com.intellij.openapi.fileTypes.UnknownFileType;
import consulo.application.progress.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import com.intellij.openapi.util.Getter;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.FilePathsHelper;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeGoToChangePopupAction;
import com.intellij.openapi.vcs.changes.patch.ApplyPatchForBaseRevisionTexts;
import com.intellij.openapi.vcs.changes.patch.PatchDiffRequestFactory;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class DiffShelvedChangesAction extends AnAction implements DumbAware {
  public void update(final AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e.getDataContext()));
  }

  public void actionPerformed(final AnActionEvent e) {
    showShelvedChangesDiff(e.getDataContext());
  }

  public static boolean isEnabled(final DataContext dc) {
    final Project project = dc.getData(CommonDataKeys.PROJECT);
    if (project == null) return false;

    ShelvedChangeList[] changeLists = dc.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    if (changeLists == null) {
      changeLists = dc.getData(ShelvedChangesViewManager.SHELVED_RECYCLED_CHANGELIST_KEY);
    }
    if (changeLists == null || changeLists.length != 1) return false;

    return true;
  }

  public static void showShelvedChangesDiff(final DataContext dc) {
    final Project project = dc.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;

    ShelvedChangeList[] changeLists = dc.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    if (changeLists == null) {
      changeLists = dc.getData(ShelvedChangesViewManager.SHELVED_RECYCLED_CHANGELIST_KEY);
    }
    if (changeLists == null || changeLists.length != 1) return;

    final List<ShelvedChange> textChanges = changeLists[0].getChanges(project);
    final List<ShelvedBinaryFile> binaryChanges = changeLists[0].getBinaryFiles();

    final List<MyDiffRequestProducer> diffRequestProducers = new ArrayList<>();

    processTextChanges(project, textChanges, diffRequestProducers);
    processBinaryFiles(project, binaryChanges, diffRequestProducers);

    Collections.sort(diffRequestProducers, ChangeDiffRequestComparator.getInstance());

    // selected changes inside lists
    final Set<Object> selectedChanges = new HashSet<>();
    selectedChanges.addAll(ContainerUtil.notNullize(dc.getData(ShelvedChangesViewManager.SHELVED_CHANGE_KEY)));
    selectedChanges.addAll(ContainerUtil.notNullize(dc.getData(ShelvedChangesViewManager.SHELVED_BINARY_FILE_KEY)));

    int index = 0;
    for (int i = 0; i < diffRequestProducers.size(); i++) {
      MyDiffRequestProducer producer = diffRequestProducers.get(i);
      if (selectedChanges.contains(producer.getBinaryChange()) || selectedChanges.contains(producer.getTextChange())) {
        index = i;
        break;
      }
    }

    MyDiffRequestChain chain = new MyDiffRequestChain(diffRequestProducers, index);
    DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.FRAME);
  }

  private static class ChangeDiffRequestComparator implements Comparator<DiffRequestProducer> {
    private final static ChangeDiffRequestComparator ourInstance = new ChangeDiffRequestComparator();

    public static ChangeDiffRequestComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(DiffRequestProducer o1, DiffRequestProducer o2) {
      return FilePathsHelper.convertPath(o1.getName()).compareTo(FilePathsHelper.convertPath(o2.getName()));
    }
  }

  private static void processBinaryFiles(@Nonnull final Project project,
                                         @Nonnull List<ShelvedBinaryFile> files,
                                         @Nonnull List<MyDiffRequestProducer> diffRequestProducers) {
    final String base = project.getBaseDir().getPath();
    for (final ShelvedBinaryFile shelvedChange : files) {
      final File file = new File(base, shelvedChange.AFTER_PATH == null ? shelvedChange.BEFORE_PATH : shelvedChange.AFTER_PATH);
      final FilePath filePath = VcsUtil.getFilePath(file);
      diffRequestProducers.add(new MyDiffRequestProducer(shelvedChange, filePath) {
        @Nonnull
        @Override
        public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
          Change change = shelvedChange.createChange(project);
          return PatchDiffRequestFactory.createDiffRequest(project, change, getName(), context, indicator);
        }
      });
    }
  }

  private static void processTextChanges(@Nonnull final Project project,
                                         @Nonnull List<ShelvedChange> changesFromFirstList,
                                         @Nonnull List<MyDiffRequestProducer> diffRequestProducers) {
    final String base = project.getBasePath();
    final ApplyPatchContext patchContext = new ApplyPatchContext(project.getBaseDir(), 0, false, false);
    final PatchesPreloader preloader = new PatchesPreloader(project);

    for (final ShelvedChange shelvedChange : changesFromFirstList) {
      final String beforePath = shelvedChange.getBeforePath();
      final String afterPath = shelvedChange.getAfterPath();
      final FilePath filePath = VcsUtil.getFilePath(new File(base, afterPath == null ? beforePath : afterPath));
      final boolean isNewFile = FileStatus.ADDED.equals(shelvedChange.getFileStatus());

      final VirtualFile file; // isNewFile -> parent directory, !isNewFile -> file
      try {
        file = ApplyFilePatchBase.findPatchTarget(patchContext, beforePath, afterPath, isNewFile);
        if (!isNewFile && (file == null || !file.exists())) throw new FileNotFoundException(beforePath);
      }
      catch (IOException e) {
        diffRequestProducers.add(new MyDiffRequestProducer(shelvedChange, filePath) {
          @Nonnull
          @Override
          public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
                  throws DiffRequestProducerException, ProcessCanceledException {
            throw new DiffRequestProducerException("Cannot find base for '" + (beforePath != null ? beforePath : afterPath) + "'");
          }
        });
        continue;
      }

      diffRequestProducers.add(new MyDiffRequestProducer(shelvedChange, filePath) {
        @Nonnull
        @Override
        public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
                throws DiffRequestProducerException, ProcessCanceledException {
          if (!isNewFile && file.getFileType() == UnknownFileType.INSTANCE) {
            return new UnknownFileTypeDiffRequest(file, getName());
          }

          if (shelvedChange.isConflictingChange(project)) {
            try {
              final CommitContext commitContext = new CommitContext();
              final TextFilePatch patch = preloader.getPatch(shelvedChange, commitContext);
              final FilePath pathBeforeRename = patchContext.getPathBeforeRename(file);
              final String relativePath = patch.getAfterName() == null ? patch.getBeforeName() : patch.getAfterName();

              final Getter<CharSequence> baseContentGetter = new Getter<CharSequence>() {
                @Override
                public CharSequence get() {
                  BaseRevisionTextPatchEP baseRevisionTextPatchEP = PatchEP.EP_NAME.findExtensionOrFail(project, BaseRevisionTextPatchEP.class);
                  return baseRevisionTextPatchEP.provideContent(relativePath, commitContext);
                }
              };

              Getter<ApplyPatchForBaseRevisionTexts> getter = new Getter<ApplyPatchForBaseRevisionTexts>() {
                @Override
                public ApplyPatchForBaseRevisionTexts get() {
                  return ApplyPatchForBaseRevisionTexts.create(project, file, pathBeforeRename, patch, baseContentGetter);
                }
              };

              return PatchDiffRequestFactory.createConflictDiffRequest(project, file, patch, "Shelved Version", getter, getName(), context, indicator);
            }
            catch (VcsException e) {
              throw new DiffRequestProducerException("Can't show diff for '" + getName() + "'", e);
            }
          }
          else {
            final Change change = shelvedChange.getChange(project);
            return PatchDiffRequestFactory.createDiffRequest(project, change, getName(), context, indicator);
          }
        }
      });
    }
  }

  private static class PatchesPreloader {
    private final Map<String, List<TextFilePatch>> myFilePatchesMap;
    private final Project myProject;

    private PatchesPreloader(final Project project) {
      myProject = project;
      myFilePatchesMap = new HashMap<>();
    }

    @Nonnull
    public TextFilePatch getPatch(final ShelvedChange shelvedChange, CommitContext commitContext) throws VcsException {
      List<TextFilePatch> textFilePatches = myFilePatchesMap.get(shelvedChange.getPatchPath());
      if (textFilePatches == null) {
        try {
          textFilePatches = ShelveChangesManager.loadPatches(myProject, shelvedChange.getPatchPath(), commitContext);
        }
        catch (IOException e) {
          throw new VcsException(e);
        }
        catch (PatchSyntaxException e) {
          throw new VcsException(e);
        }
        myFilePatchesMap.put(shelvedChange.getPatchPath(), textFilePatches);
      }
      for (TextFilePatch textFilePatch : textFilePatches) {
        if (shelvedChange.getBeforePath().equals(textFilePatch.getBeforeName())) {
          return textFilePatch;
        }
      }
      throw new VcsException("Can not find patch for " + shelvedChange.getBeforePath() + " in patch file.");
    }
  }

  private static class MyDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
    @Nonnull
    private final List<MyDiffRequestProducer> myProducers;
    private int myIndex = 0;

    public MyDiffRequestChain(@Nonnull List<MyDiffRequestProducer> producers, int index) {
      myProducers = producers;
      myIndex = index;
    }

    @Nonnull
    @Override
    public List<? extends DiffRequestProducer> getRequests() {
      return myProducers;
    }

    @Override
    public int getIndex() {
      return myIndex;
    }

    @Override
    public void setIndex(int index) {
      assert index >= 0 && index < myProducers.size();
      myIndex = index;
    }

    @Nonnull
    @Override
    public AnAction createGoToChangeAction(@Nonnull Consumer<Integer> onSelected) {
      return new ChangeGoToChangePopupAction.Fake<MyDiffRequestChain>(this, myIndex, onSelected) {
        @Nonnull
        @Override
        protected FilePath getFilePath(int index) {
          return myProducers.get(index).getFilePath();
        }

        @Nonnull
        @Override
        protected FileStatus getFileStatus(int index) {
          return myProducers.get(index).getFileStatus();
        }
      };
    }
  }

  private static abstract class MyDiffRequestProducer implements DiffRequestProducer {
    @javax.annotation.Nullable
    private final ShelvedChange myTextChange;
    @Nullable private final ShelvedBinaryFile myBinaryChange;
    @Nonnull
    private final FilePath myFilePath;

    public MyDiffRequestProducer(@Nonnull ShelvedChange textChange, @Nonnull FilePath filePath) {
      myBinaryChange = null;
      myTextChange = textChange;
      myFilePath = filePath;
    }

    public MyDiffRequestProducer(@Nonnull ShelvedBinaryFile binaryChange, @Nonnull FilePath filePath) {
      myBinaryChange = binaryChange;
      myTextChange = null;
      myFilePath = filePath;
    }

    @Nullable
    public ShelvedChange getTextChange() {
      return myTextChange;
    }

    @javax.annotation.Nullable
    public ShelvedBinaryFile getBinaryChange() {
      return myBinaryChange;
    }

    @Nonnull
    @Override
    public String getName() {
      return FileUtil.toSystemDependentName(getFilePath().getPath());
    }

    @Nonnull
    protected FileStatus getFileStatus() {
      if (myTextChange != null) {
        return myTextChange.getFileStatus();
      }
      else {
        assert myBinaryChange != null;
        return myBinaryChange.getFileStatus();
      }
    }

    @Nonnull
    public FilePath getFilePath() {
      return myFilePath;
    }
  }
}
