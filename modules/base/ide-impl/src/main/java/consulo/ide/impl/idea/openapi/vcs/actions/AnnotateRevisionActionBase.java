package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AnnotateRevisionActionBase extends AnAction {
  public AnnotateRevisionActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  @Nullable
  protected abstract AbstractVcs getVcs(@Nonnull AnActionEvent e);

  @Nullable
  protected abstract VirtualFile getFile(@Nonnull AnActionEvent e);

  @Nullable
  protected abstract VcsFileRevision getFileRevision(@Nonnull AnActionEvent e);

  @Nullable
  protected Editor getEditor(@Nonnull AnActionEvent e) {
    return null;
  }

  protected int getAnnotatedLine(@Nonnull AnActionEvent e) {
    Editor editor = getEditor(e);
    return editor == null ? 0 : editor.getCaretModel().getLogicalPosition().line;
  }

  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  @RequiredUIAccess
  public boolean isEnabled(@Nonnull AnActionEvent e) {
    if (e.getData(Project.KEY) == null) return false;

    VcsFileRevision fileRevision = getFileRevision(e);
    if (fileRevision == null) return false;

    VirtualFile file = getFile(e);
    if (file == null) return false;

    AbstractVcs vcs = getVcs(e);
    if (vcs == null) return false;

    AnnotationProvider provider = vcs.getAnnotationProvider();
    if (provider == null || !provider.isAnnotationValid(fileRevision)) return false;

    return !VcsAnnotateUtil.getBackgroundableLock(vcs.getProject(), file).isLocked();
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final VcsFileRevision fileRevision = getFileRevision(e);
    final VirtualFile file = getFile(e);
    final AbstractVcs vcs = getVcs(e);
    assert vcs != null;
    assert file != null;
    assert fileRevision != null;

    final Editor editor = getEditor(e);
    final CharSequence oldContent = editor == null ? null : editor.getDocument().getImmutableCharSequence();
    final int oldLine = getAnnotatedLine(e);

    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    assert annotationProvider != null;

    final Ref<FileAnnotation> fileAnnotationRef = new Ref<>();
    final Ref<Integer> newLineRef = new Ref<>();
    final Ref<VcsException> exceptionRef = new Ref<>();

    VcsAnnotateUtil.getBackgroundableLock(vcs.getProject(), file).lock();

    Semaphore semaphore = new Semaphore(0);
    AtomicBoolean shouldOpenEditorInSync = new AtomicBoolean(true);

    ProgressManager.getInstance().run(new Task.Backgroundable(vcs.getProject(), VcsLocalize.retrievingAnnotations().get(), true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          FileAnnotation fileAnnotation = annotationProvider.annotate(file, fileRevision);

          int newLine = translateLine(oldContent, fileAnnotation.getAnnotatedContent(), oldLine);

          fileAnnotationRef.set(fileAnnotation);
          newLineRef.set(newLine);

          shouldOpenEditorInSync.set(false);
          semaphore.release();
        }
        catch (VcsException e) {
          exceptionRef.set(e);
        }
      }

      @Override
      @RequiredUIAccess
      public void onFinished() {
        VcsAnnotateUtil.getBackgroundableLock(vcs.getProject(), file).unlock();
      }

      @Override
      @RequiredUIAccess
      public void onSuccess() {
        if (!exceptionRef.isNull()) {
          AbstractVcsHelper.getInstance((Project)myProject).showError(exceptionRef.get(), VcsLocalize.operationNameAnnotate().get());
        }
        if (fileAnnotationRef.isNull()) return;

        AbstractVcsHelper.getInstance((Project)myProject).showAnnotation(fileAnnotationRef.get(), file, vcs, newLineRef.get());
      }
    });

    try {
      semaphore.tryAcquire(ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS, TimeUnit.MILLISECONDS);

      // We want to let Backgroundable task open editor if it was fast enough.
      // This will remove blinking on editor opening (step 1 - editor opens, step 2 - annotations are shown).
      if (shouldOpenEditorInSync.get()) {
        CharSequence content = LoadTextUtil.loadText(file);
        int newLine = translateLine(oldContent, content, oldLine);

        OpenFileDescriptorImpl openFileDescriptor = new OpenFileDescriptorImpl(vcs.getProject(), file, newLine, 0);
        FileEditorManager.getInstance(vcs.getProject()).openTextEditor(openFileDescriptor, true);
      }
    }
    catch (InterruptedException ignore) {
    }
  }

  private static int translateLine(@Nullable CharSequence oldContent, @Nullable CharSequence newContent, int line) {
    if (oldContent == null || newContent == null) return line;
    try {
      return Diff.translateLine(oldContent, newContent, line, true);
    }
    catch (FilesTooBigForDiffException ignore) {
      return line;
    }
  }
}
