/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static consulo.util.lang.ObjectUtil.assertNotNull;

public class AnnotateLocalFileAction {
  private static final Logger LOG = Logger.getInstance(AnnotateLocalFileAction.class);

  public static boolean isEnabled(AnActionEvent e) {
    VcsContext context = VcsContextFactory.getInstance().createContextOn(e);

    Project project = context.getProject();
    if (project == null || project.isDisposed()) return false;

    VirtualFile file = context.getSelectedFile();
    if (file == null || file.isDirectory() || file.getFileType().isBinary()) return false;

    final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return false;

    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    if (annotationProvider == null) return false;

    final FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.ADDED || fileStatus == FileStatus.IGNORED) {
      return false;
    }

    return true;
  }

  public static boolean isSuspended(AnActionEvent e) {
    VirtualFile file = assertNotNull(VcsContextFactory.getInstance().createContextOn(e).getSelectedFile());
    return VcsAnnotateUtil.getBackgroundableLock(e.getRequiredData(Project.KEY), file).isLocked();
  }

  public static boolean isAnnotated(AnActionEvent e) {
    VcsContext context = VcsContextFactory.getInstance().createContextOn(e);

    Editor editor = context.getEditor();
    if (editor != null) {
      return editor.getGutter().isAnnotationsShown();
    }

    return ContainerUtil.exists(getEditors(context), editor1 -> editor1.getGutter().isAnnotationsShown());
  }

  public static void perform(AnActionEvent e, boolean selected) {
    final VcsContext context = VcsContextFactory.getInstance().createContextOn(e);

    if (!selected) {
      for (Editor editor : getEditors(context)) {
        editor.getGutter().closeAllAnnotations();
      }
    }
    else {
      Project project = assertNotNull(context.getProject());
      VirtualFile selectedFile = assertNotNull(context.getSelectedFile());

      Editor editor = context.getEditor();
      if (editor == null) {
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).openFile(selectedFile, false);
        for (FileEditor fileEditor : fileEditors) {
          if (fileEditor instanceof TextEditor) {
            editor = ((TextEditor)fileEditor).getEditor();
          }
        }
      }
      LOG.assertTrue(editor != null);
      doAnnotate(editor, project);
    }
  }

  private static void doAnnotate(@Nonnull final Editor editor, @Nonnull final Project project) {
    final VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) return;

    final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return;

    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    assert annotationProvider != null;

    final Ref<FileAnnotation> fileAnnotationRef = new Ref<>();
    final Ref<VcsException> exceptionRef = new Ref<>();

    VcsAnnotateUtil.getBackgroundableLock(project, file).lock();

    final Task.Backgroundable annotateTask = new Task.Backgroundable(project, VcsLocalize.retrievingAnnotations().get(), true) {
      @Override
      public void run(final @Nonnull ProgressIndicator indicator) {
        try {
          fileAnnotationRef.set(annotationProvider.annotate(file));
        }
        catch (VcsException e) {
          exceptionRef.set(e);
        }
        catch (ProcessCanceledException pce) {
          throw pce;
        }
        catch (Throwable t) {
          exceptionRef.set(new VcsException(t));
        }
      }

      @Override
      public void onCancel() {
        onSuccess();
      }

      @Override
      public void onSuccess() {
        VcsAnnotateUtil.getBackgroundableLock(project, file).unlock();

        if (!exceptionRef.isNull()) {
          LOG.warn(exceptionRef.get());
          AbstractVcsHelper.getInstance(project).showErrors(Collections.singletonList(exceptionRef.get()), VcsLocalize.messageTitleAnnotate().get());
        }

        if (!fileAnnotationRef.isNull()) {
          AnnotateToggleAction.doAnnotate(editor, project, file, fileAnnotationRef.get(), vcs);
        }
      }
    };
    ProgressManager.getInstance().run(annotateTask);
  }

  @Nonnull
  private static List<Editor> getEditors(@Nonnull VcsContext context) {
    Project project = assertNotNull(context.getProject());
    VirtualFile file = assertNotNull(context.getSelectedFile());
    return VcsAnnotateUtil.getEditors(project, file);
  }

}
