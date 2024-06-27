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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.diff.DiffContentFactory;
import consulo.ide.impl.idea.diff.DiffContentFactoryEx;
import consulo.diff.DiffManager;
import consulo.ide.impl.idea.diff.DiffRequestFactory;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.DiffUserDataKeys;
import consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx;
import consulo.diff.util.Side;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.change.BinaryContentRevision;
import consulo.versionControlSystem.change.ByteBackedContentRevision;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.diff.ItemLatestState;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.impl.BackgroundableActionEnabledHandler;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

// TODO: remove duplication with ChangeDiffRequestPresentable
public abstract class DiffActionExecutor {
  protected final DiffProvider myDiffProvider;
  protected final VirtualFile mySelectedFile;
  protected final Project myProject;
  private final BackgroundableActionEnabledHandler myHandler;

  protected DiffActionExecutor(final DiffProvider diffProvider, final VirtualFile selectedFile, final Project project,
                               final VcsBackgroundableActions actionKey) {
    final ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
    myHandler = vcsManager.getBackgroundableActionHandler(actionKey);
    myDiffProvider = diffProvider;
    mySelectedFile = selectedFile;
    myProject = project;
  }

  @Nullable
  protected DiffContent createRemote(final VcsRevisionNumber revisionNumber) throws IOException, VcsException {
    final ContentRevision fileRevision = myDiffProvider.createFileContent(revisionNumber, mySelectedFile);
    if (fileRevision == null) return null;
    DiffContentFactoryEx contentFactory = DiffContentFactoryEx.getInstanceEx();

    DiffContent diffContent;
    if (fileRevision instanceof BinaryContentRevision) {
      FilePath filePath = fileRevision.getFile();
      final byte[] content = ((BinaryContentRevision)fileRevision).getBinaryContent();
      if (content == null) return null;

      diffContent = contentFactory.createBinary(myProject, content, filePath.getFileType(), filePath.getName());
    }
    else if (fileRevision instanceof ByteBackedContentRevision) {
      byte[] content = ((ByteBackedContentRevision)fileRevision).getContentAsBytes();
      if (content == null) throw new VcsException("Failed to load content");
      diffContent = contentFactory.createFromBytes(myProject, content, fileRevision.getFile());
    }
    else {
      String content = fileRevision.getContent();
      if (content == null) throw new VcsException("Failed to load content");
      diffContent = contentFactory.create(myProject, content, fileRevision.getFile());
    }

    diffContent.putUserData(DiffUserDataKeysEx.REVISION_INFO, Pair.create(fileRevision.getFile(), fileRevision.getRevisionNumber()));
    return diffContent;
  }

  public void showDiff() {
    final Ref<VcsException> exceptionRef = new Ref<>();
    final Ref<DiffRequest> requestRef = new Ref<>();

    final Task.Backgroundable task = new Task.Backgroundable(myProject,
                                                             VcsBundle.message("show.diff.progress.title.detailed",
                                                                               mySelectedFile.getPresentableUrl()),
                                                             true) {

      public void run(@Nonnull ProgressIndicator indicator) {
        final VcsRevisionNumber revisionNumber = getRevisionNumber();
        try {
          if (revisionNumber == null) {
            return;
          }
          DiffContent content1 = createRemote(revisionNumber);
          if (content1 == null) return;
          DiffContent content2 = DiffContentFactory.getInstance().create((Project)myProject, mySelectedFile);

          String title = DiffRequestFactory.getInstance().getTitle(mySelectedFile);

          boolean inverted = false;
          String title1;
          String title2;
          final FileStatus status = FileStatusManager.getInstance((Project)myProject).getStatus(mySelectedFile);
          if (status == null || FileStatus.NOT_CHANGED.equals(status) || FileStatus.UNKNOWN.equals(status) ||
              FileStatus.IGNORED.equals(status)) {
            final VcsRevisionNumber currentRevision = myDiffProvider.getCurrentRevision(mySelectedFile);

            inverted = revisionNumber.compareTo(currentRevision) > 0;
            title1 = revisionNumber.asString();
            title2 = VcsBundle.message("diff.title.local.with.number", currentRevision.asString());
          }
          else {
            title1 = revisionNumber.asString();
            title2 = VcsBundle.message("diff.title.local");
          }

          Integer line = null;
          if (content2 instanceof DocumentContent) {
            Editor[] editors = EditorFactory.getInstance().getEditors(((DocumentContent)content2).getDocument(), (Project)myProject);
            if (editors.length != 0) line = editors[0].getCaretModel().getLogicalPosition().line;
          }

          if (inverted) {
            SimpleDiffRequest request = new SimpleDiffRequest(title, content2, content1, title2, title1);
            if (line != null) request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, consulo.util.lang.Pair.create(Side.LEFT, line));
            request.putUserData(DiffUserDataKeys.MASTER_SIDE, Side.LEFT);
            requestRef.set(request);
          }
          else {
            SimpleDiffRequest request = new SimpleDiffRequest(title, content1, content2, title1, title2);
            if (line != null) request.putUserData(DiffUserDataKeys.SCROLL_TO_LINE, consulo.util.lang.Pair.create(Side.RIGHT, line));
            request.putUserData(DiffUserDataKeys.MASTER_SIDE, Side.RIGHT);
            requestRef.set(request);
          }
        }
        catch (ProcessCanceledException e) {
          //ignore
        }
        catch (VcsException e) {
          exceptionRef.set(e);
        }
        catch (IOException e) {
          exceptionRef.set(new VcsException(e));
        }
      }

      @RequiredUIAccess
      @Override
      public void onCancel() {
        onSuccess();
      }

      @RequiredUIAccess
      @Override
      public void onSuccess() {
        myHandler.completed(VcsBackgroundableActions.keyFrom(mySelectedFile));

        if (!exceptionRef.isNull()) {
          AbstractVcsHelper.getInstance((Project)myProject).showError(exceptionRef.get(), VcsBundle.message("message.title.diff"));
          return;
        }
        if (!requestRef.isNull()) {
          DiffManager.getInstance().showDiff((Project)myProject, requestRef.get());
        }
      }
    };

    myHandler.register(VcsBackgroundableActions.keyFrom(mySelectedFile));
    ProgressManager.getInstance().run(task);
  }

  public static void showDiff(final DiffProvider diffProvider, final VcsRevisionNumber revisionNumber, final VirtualFile selectedFile,
                              final Project project, final VcsBackgroundableActions actionKey) {
    final DiffActionExecutor executor = new CompareToFixedExecutor(diffProvider, selectedFile, project, revisionNumber, actionKey);
    executor.showDiff();
  }

  @Nullable
  protected abstract VcsRevisionNumber getRevisionNumber();

  public static class CompareToFixedExecutor extends DiffActionExecutor {
    private final VcsRevisionNumber myNumber;

    public CompareToFixedExecutor(final DiffProvider diffProvider,
                                  final VirtualFile selectedFile, final Project project, final VcsRevisionNumber number,
                                  final VcsBackgroundableActions actionKey) {
      super(diffProvider, selectedFile, project, actionKey);
      myNumber = number;
    }

    protected VcsRevisionNumber getRevisionNumber() {
      return myNumber;
    }
  }

  public static class CompareToCurrentExecutor extends DiffActionExecutor {
    public CompareToCurrentExecutor(final DiffProvider diffProvider, final VirtualFile selectedFile, final Project project,
                                    final VcsBackgroundableActions actionKey) {
      super(diffProvider, selectedFile, project, actionKey);
    }

    @Nullable
    protected VcsRevisionNumber getRevisionNumber() {
      return myDiffProvider.getCurrentRevision(mySelectedFile);
    }
  }

  public static class DeletionAwareExecutor extends DiffActionExecutor {
    private boolean myFileStillExists;

    public DeletionAwareExecutor(final DiffProvider diffProvider,
                                 final VirtualFile selectedFile, final Project project, final VcsBackgroundableActions actionKey) {
      super(diffProvider, selectedFile, project, actionKey);
    }

    protected VcsRevisionNumber getRevisionNumber() {
      final ItemLatestState itemState = myDiffProvider.getLastRevision(mySelectedFile);
      if (itemState == null) {
        return null;
      }
      myFileStillExists = itemState.isItemExists();
      return itemState.getNumber();
    }

    @Override
    protected DiffContent createRemote(final VcsRevisionNumber revisionNumber) throws IOException, VcsException {
      if (myFileStillExists) {
        return super.createRemote(revisionNumber);
      } else {
        return DiffContentFactory.getInstance().createEmpty();
      }
    }
  }
}
