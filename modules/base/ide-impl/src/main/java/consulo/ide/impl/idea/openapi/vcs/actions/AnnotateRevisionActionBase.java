package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.language.impl.internal.psi.LoadTextUtil;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  public boolean isEnabled(@Nonnull AnActionEvent e) {
    if (e.getData(CommonDataKeys.PROJECT) == null) return false;

    VcsFileRevision fileRevision = getFileRevision(e);
    if (fileRevision == null) return false;

    VirtualFile file = getFile(e);
    if (file == null) return false;

    AbstractVcs vcs = getVcs(e);
    if (vcs == null) return false;

    AnnotationProvider provider = vcs.getAnnotationProvider();
    if (provider == null || !provider.isAnnotationValid(fileRevision)) return false;

    if (VcsAnnotateUtil.getBackgroundableLock(vcs.getProject(), file).isLocked()) return false;

    return true;
  }

  @Override
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

    ProgressManager.getInstance().run(new Task.Backgroundable(vcs.getProject(), VcsBundle.message("retrieving.annotations"), true) {
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
      public void onFinished() {
        VcsAnnotateUtil.getBackgroundableLock(vcs.getProject(), file).unlock();
      }

      @Override
      public void onSuccess() {
        if (!exceptionRef.isNull()) {
          AbstractVcsHelper.getInstance((consulo.project.Project)myProject).showError(exceptionRef.get(), VcsBundle.message("operation.name.annotate"));
        }
        if (fileAnnotationRef.isNull()) return;

        AbstractVcsHelper.getInstance((consulo.project.Project)myProject).showAnnotation(fileAnnotationRef.get(), file, vcs, newLineRef.get());
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
