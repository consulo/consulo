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
package consulo.ide.impl.idea.openapi.vcs.update;

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.content.DiffContent;
import consulo.diff.internal.DiffContentFactoryEx;
import consulo.diff.internal.DiffRequestFactoryEx;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.ide.impl.idea.diff.actions.impl.GoToChangePopupBuilder;
import consulo.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ChangeGoToChangePopupAction;
import consulo.localHistory.ByteContent;
import consulo.localHistory.Label;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    final Project project = dc.getData(Project.KEY);
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

    final Project project = dc.getData(Project.KEY);
    final Iterable<Pair<FilePath, FileStatus>> iterable = e.getRequiredData(VcsDataKeys.UPDATE_VIEW_FILES_ITERABLE);
    final Label before = (Label)e.getRequiredData(VcsDataKeys.LABEL_BEFORE);
    final Label after = (Label)e.getRequiredData(VcsDataKeys.LABEL_AFTER);
    final FilePath selectedUrl = dc.getData(VcsDataKeys.UPDATE_VIEW_SELECTED_PATH);

    MyDiffRequestChain requestChain = new MyDiffRequestChain(project, iterable, before, after, selectedUrl);
    DiffManager.getInstance().showDiff(project, requestChain, DiffDialogHints.FRAME);
  }

  private static class MyDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
    @Nullable
    private final Project myProject;
    @Nonnull
    private final Label myBefore;
    @Nonnull
    private final Label myAfter;
    @Nonnull
    private final List<MyDiffRequestProducer> myRequests = new ArrayList<>();

    private int myIndex;

    public MyDiffRequestChain(
      @Nullable Project project,
      @Nonnull Iterable<Pair<FilePath, FileStatus>> iterable,
      @Nonnull Label before,
      @Nonnull Label after,
      @Nullable FilePath filePath
    ) {
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
      return new ChangeGoToChangePopupAction.Fake<>(this, myIndex, onSelected) {
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

          String title = DiffRequestFactoryEx.getInstanceEx().getContentTitle(myFilePath);
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
