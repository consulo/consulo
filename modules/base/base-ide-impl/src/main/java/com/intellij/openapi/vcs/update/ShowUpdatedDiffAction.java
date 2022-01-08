/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.update;

import com.intellij.diff.DiffContentFactoryEx;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactoryImpl;
import com.intellij.diff.actions.impl.GoToChangePopupBuilder;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.history.ByteContent;
import com.intellij.history.Label;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeGoToChangePopupAction;
import com.intellij.util.Consumer;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShowUpdatedDiffAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    final DataContext dc = e.getDataContext();

    final Presentation presentation = e.getPresentation();

    //presentation.setVisible(isVisible(dc));
    presentation.setEnabled(isVisible(dc) && isEnabled(dc));
  }

  private boolean isVisible(final DataContext dc) {
    final Project project = dc.getData(CommonDataKeys.PROJECT);
    return (project != null) && (dc.getData(VcsDataKeys.LABEL_BEFORE) != null) && (dc.getData(VcsDataKeys.LABEL_AFTER) != null);
  }

  private boolean isEnabled(final DataContext dc) {
    final Iterable<Pair<FilePath, FileStatus>> iterable = dc.getData(VcsDataKeys.UPDATE_VIEW_FILES_ITERABLE);
    return iterable != null;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final DataContext dc = e.getDataContext();
    if ((!isVisible(dc)) || (!isEnabled(dc))) return;

    final Project project = dc.getData(CommonDataKeys.PROJECT);
    final Iterable<Pair<FilePath, FileStatus>> iterable = e.getRequiredData(VcsDataKeys.UPDATE_VIEW_FILES_ITERABLE);
    final Label before = (Label)e.getRequiredData(VcsDataKeys.LABEL_BEFORE);
    final Label after = (Label)e.getRequiredData(VcsDataKeys.LABEL_AFTER);
    final FilePath selectedUrl = dc.getData(VcsDataKeys.UPDATE_VIEW_SELECTED_PATH);

    MyDiffRequestChain requestChain = new MyDiffRequestChain(project, iterable, before, after, selectedUrl);
    DiffManager.getInstance().showDiff(project, requestChain, DiffDialogHints.FRAME);
  }

  private static class MyDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
    @javax.annotation.Nullable
    private final Project myProject;
    @Nonnull
    private final Label myBefore;
    @Nonnull
    private final Label myAfter;
    @Nonnull
    private final List<MyDiffRequestProducer> myRequests = new ArrayList<>();

    private int myIndex;

    public MyDiffRequestChain(@javax.annotation.Nullable Project project,
                              @Nonnull Iterable<Pair<FilePath, FileStatus>> iterable,
                              @Nonnull Label before,
                              @Nonnull Label after,
                              @javax.annotation.Nullable FilePath filePath) {
      myProject = project;
      myBefore = before;
      myAfter = after;

      int selected = -1;
      for (Pair<FilePath, FileStatus> pair : iterable) {
        if (selected == -1 && pair.first.equals(filePath)) selected = myRequests.size();
        myRequests.add(new MyDiffRequestProducer(pair.first, pair.second));
      }
      if (selected != -1) myIndex = selected;
    }

    @Nonnull
    @Override
    public List<MyDiffRequestProducer> getRequests() {
      return myRequests;
    }

    @Override
    public int getIndex() {
      return myIndex;
    }

    @Override
    public void setIndex(int index) {
      myIndex = index;
    }

    @Nonnull
    @Override
    public AnAction createGoToChangeAction(@Nonnull Consumer<Integer> onSelected) {
      return new ChangeGoToChangePopupAction.Fake<MyDiffRequestChain>(this, myIndex, onSelected) {
        @Nonnull
        @Override
        protected FilePath getFilePath(int index) {
          return myRequests.get(index).getFilePath();
        }

        @Nonnull
        @Override
        protected FileStatus getFileStatus(int index) {
          return myRequests.get(index).getFileStatus();
        }
      };
    }

    private class MyDiffRequestProducer implements DiffRequestProducer {
      @Nonnull
      private final FileStatus myFileStatus;
      @Nonnull
      private final FilePath myFilePath;

      public MyDiffRequestProducer(@Nonnull FilePath filePath, @Nonnull FileStatus fileStatus) {
        myFilePath = filePath;
        myFileStatus = fileStatus;
      }

      @Nonnull
      @Override
      public String getName() {
        return getFilePath().getPath();
      }

      @Nonnull
      public FilePath getFilePath() {
        return myFilePath;
      }

      @Nonnull
      public FileStatus getFileStatus() {
        return myFileStatus;
      }

      @Nonnull
      @Override
      public DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator)
              throws DiffRequestProducerException, ProcessCanceledException {
        try {
          DiffContent content1;
          DiffContent content2;

          DiffContentFactoryEx contentFactory = DiffContentFactoryEx.getInstanceEx();

          if (FileStatus.ADDED.equals(myFileStatus)) {
            content1 = contentFactory.createEmpty();
          }
          else {
            byte[] bytes1 = loadContent(myFilePath, myBefore);
            content1 = contentFactory.createFromBytes(myProject, bytes1, myFilePath);
          }

          if (FileStatus.DELETED.equals(myFileStatus)) {
            content2 = contentFactory.createEmpty();
          }
          else {
            byte[] bytes2 = loadContent(myFilePath, myAfter);
            content2 = contentFactory.createFromBytes(myProject, bytes2, myFilePath);
          }

          String title = DiffRequestFactoryImpl.getContentTitle(myFilePath);
          return new SimpleDiffRequest(title, content1, content2, "Before update", "After update");
        }
        catch (IOException e) {
          throw new DiffRequestProducerException("Can't load content", e);
        }
      }
    }

    @Nonnull
    private static byte[] loadContent(@Nonnull FilePath filePointer, @Nonnull Label label) throws DiffRequestProducerException {
      String path = filePointer.getPresentableUrl();
      ByteContent byteContent = label.getByteContent(FileUtil.toSystemIndependentName(path));

      if (byteContent == null || byteContent.isDirectory() || byteContent.getBytes() == null) {
        throw new DiffRequestProducerException("Can't load content");
      }

      return byteContent.getBytes();
    }
  }
}
