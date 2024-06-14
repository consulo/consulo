// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.impl.internal.progress.StandardProgressIndicatorBase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.ProcessCanceledException;
import consulo.component.util.PluginExceptionUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.ide.ServiceManager;
import consulo.language.ast.ASTNode;
import consulo.language.ast.FileASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.psi.BooleanRunnable;
import consulo.language.impl.internal.psi.ChangedPsiRangeUtil;
import consulo.language.impl.internal.psi.DocumentCommitProcessor;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.diff.BlockSupport;
import consulo.language.impl.internal.psi.diff.BlockSupportImpl;
import consulo.language.impl.internal.psi.diff.DiffLog;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.concurrent.ExecutorService;

@ServiceImpl
@Singleton
public final class DocumentCommitThread implements Disposable, DocumentCommitProcessor {
  private static final Logger LOG = Logger.getInstance(DocumentCommitThread.class);
  private static final String SYNC_COMMIT_REASON = "Sync commit";

  private final ExecutorService executor;
  private volatile boolean isDisposed;

  static DocumentCommitThread getInstance() {
    return (DocumentCommitThread)ServiceManager.getService(DocumentCommitProcessor.class);
  }

  @Inject
  DocumentCommitThread(ApplicationConcurrency applicationConcurrency) {
    executor = applicationConcurrency.createBoundedApplicationPoolExecutor("Document Committing Pool", 1, this);
  }

  @Override
  public void dispose() {
    isDisposed = true;
  }

  @Override
  public void commitAsynchronously(@Nonnull final Project project,
                                   @Nonnull final Document document,
                                   @NonNls @Nonnull Object reason,
                                   @Nonnull ModalityState modality) {
    assert !isDisposed : "already disposed";
    if (!project.isInitialized()) return;

    PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
    if (documentManager.isEventSystemEnabled(document)) {
      Application.get().assertWriteAccessAllowed();
    }

    PsiFile psiFile = documentManager.getCachedPsiFile(document);
    if (psiFile == null || psiFile instanceof PsiCompiledElement) return;

    CommitTask task =
      new CommitTask(project, document, reason, modality, documentManager.getLastCommittedText(document));
    ReadAction
      .nonBlocking(() -> commitUnderProgress(task, false))
      .expireWhen(() -> project.isDisposed() || isDisposed || !documentManager.isInUncommittedSet(document) || !task.isStillValid())
      .coalesceBy(task)
      .finishOnUiThread(application -> modality, Runnable::run)
      .submit(executor);
  }

  @SuppressWarnings("unused")
  private void log(Project project, @NonNls String msg, @Nullable CommitTask task, @NonNls Object... args) {
    //System.out.println(msg + "; task: "+task + "; args: "+StringUtil.first(java.util.Arrays.toString(args), 80, true));
  }

  @Override
  public void commitSynchronously(@Nonnull Document document, @Nonnull Project project, @Nonnull PsiFile psiFile) {
    assert !isDisposed;

    if (!project.isInitialized() && !project.isDefault()) {
      @NonNls String s = project + "; Disposed: " + project.isDisposed() + "; Open: " + project.isOpen();
      try {
        Disposer.dispose(project);
      }
      catch (Throwable ignored) {
        // do not fill log with endless exceptions
      }
      throw new RuntimeException(s);
    }

    CommitTask task = new CommitTask(project, document, SYNC_COMMIT_REASON, project.getApplication().getDefaultModalityState(),
                                     PsiDocumentManager.getInstance(project).getLastCommittedText(document));

    commitUnderProgress(task, true).run();
  }

  // returns finish commit Runnable (to be invoked later in EDT) or null on failure
  @Nonnull
  private Runnable commitUnderProgress(@Nonnull CommitTask task, boolean synchronously) {
    final Document document = task.getDocument();
    final Project project = task.project;
    final PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
    final List<BooleanRunnable> finishProcessors = new SmartList<>();
    List<BooleanRunnable> reparseInjectedProcessors = new SmartList<>();

    FileViewProvider viewProvider = documentManager.getCachedViewProvider(document);
    if (viewProvider == null) {
      finishProcessors.add(handleCommitWithoutPsi(documentManager, task));
    }
    else {
      for (PsiFile file : viewProvider.getAllFiles()) {
        FileASTNode oldFileNode = file.getNode();
        ProperTextRange changedPsiRange = ChangedPsiRangeUtil
          .getChangedPsiRange(file, task.document, task.myLastCommittedText, document.getImmutableCharSequence());
        if (changedPsiRange != null) {
          BooleanRunnable finishProcessor = doCommit(task, file, oldFileNode, changedPsiRange, reparseInjectedProcessors);
          finishProcessors.add(finishProcessor);
        }
      }
    }

    return createFinishCommitRunnable(task, synchronously, finishProcessors, reparseInjectedProcessors);
  }

  @Nonnull
  private Runnable createFinishCommitRunnable(@Nonnull final CommitTask task,
                                              final boolean synchronously,
                                              @Nonnull List<? extends BooleanRunnable> finishProcessors,
                                              @Nonnull List<? extends BooleanRunnable> reparseInjectedProcessors) {
    return () -> {
      Document document = task.getDocument();
      Project project = task.project;
      if (project.isDisposed()) {
        return;
      }
      PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
      if (documentManager.isEventSystemEnabled(document)) {
        ApplicationManager.getApplication().assertIsDispatchThread();
      }
      boolean success = documentManager.finishCommit(document, finishProcessors, reparseInjectedProcessors,
                                                     synchronously, task.reason);
      if (synchronously) {
        assert success;
      }
      if (synchronously || success) {
        assert !documentManager.isInUncommittedSet(document);
      }
      if (success) {
        log(project, "Commit finished", task);
      }
      else {
        // add document back to the queue
        commitAsynchronously(project, document, "Re-added back", task.myCreationModality);
      }
    };
  }

  @Nonnull
  private BooleanRunnable handleCommitWithoutPsi(@Nonnull final PsiDocumentManagerBase documentManager,
                                                 @Nonnull final CommitTask task) {
    return () -> {
      log(task.project, "Finishing without PSI", task);
      Document document = task.getDocument();
      if (!task.isStillValid() || documentManager.getCachedViewProvider(document) != null) {
        return false;
      }

      documentManager.handleCommitWithoutPsi(document);
      return true;
    };
  }

  @Override
  public String toString() {
    return "Document commit thread; application: " + ApplicationManager.getApplication() + "; isDisposed: " + isDisposed;
  }

  private static class CommitTask {
    @Nonnull
    private final Document document;
    @Nonnull
    final Project project;
    private final int modificationSequence;
      // store initial document modification sequence here to check if it changed later before commit in EDT

    @Nonnull
    final Object reason;
    @Nonnull
    final ModalityState myCreationModality;
    private final CharSequence myLastCommittedText;

    CommitTask(@Nonnull final Project project,
               @Nonnull final Document document,
               @Nonnull Object reason,
               @Nonnull ModalityState modality,
               @Nonnull CharSequence lastCommittedText) {
      this.document = document;
      this.project = project;
      this.reason = reason;
      myCreationModality = modality;
      myLastCommittedText = lastCommittedText;
      modificationSequence = ((DocumentEx)document).getModificationSequence();
    }

    @NonNls
    @Override
    public String toString() {
      Document document = getDocument();
      String reasonInfo = " task reason: " + StringUtil.first(String.valueOf(reason), 180, true) +
        (isStillValid() ? "" : "; changed: old seq=" + modificationSequence + ", new seq=" + ((DocumentEx)document).getModificationSequence());
      String contextInfo = " modality: " + myCreationModality;
      return System.identityHashCode(this) + "; " + contextInfo + reasonInfo;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CommitTask)) return false;

      CommitTask task = (CommitTask)o;

      return Comparing.equal(getDocument(), task.getDocument()) && project.equals(task.project);
    }

    @Override
    public int hashCode() {
      int result = getDocument().hashCode();
      result = 31 * result + project.hashCode();
      return result;
    }

    boolean isStillValid() {
      Document document = getDocument();
      return ((DocumentEx)document).getModificationSequence() == modificationSequence;
    }

    @Nonnull
    Document getDocument() {
      return document;
    }

  }

  // returns runnable to execute under write action in AWT to finish the commit, updates "outChangedRange"
  @Nonnull
  private static BooleanRunnable doCommit(@Nonnull final CommitTask task,
                                          @Nonnull final PsiFile file,
                                          @Nonnull final FileASTNode oldFileNode,
                                          @Nonnull ProperTextRange changedPsiRange,
                                          @Nonnull List<? super BooleanRunnable> outReparseInjectedProcessors) {
    Document document = task.getDocument();
    final CharSequence newDocumentText = document.getImmutableCharSequence();

    final Boolean data = document.getUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY);
    if (data != null) {
      document.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, null);
      file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, data);
    }

    PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(task.project);

    DiffLog diffLog;
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (indicator == null) indicator = new EmptyProgressIndicator();
    try (
      BlockSupportImpl.ReparseResult result =
        BlockSupportImpl.reparse(file, oldFileNode, changedPsiRange, newDocumentText, indicator, task.myLastCommittedText)) {
      diffLog = result.log;


      List<BooleanRunnable> injectedRunnables =
        documentManager.reparseChangedInjectedFragments(document, file, changedPsiRange, indicator, result.oldRoot, result.newRoot);
      outReparseInjectedProcessors.addAll(injectedRunnables);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(e);
      return () -> {
        documentManager.forceReload(file.getViewProvider().getVirtualFile(), file.getViewProvider());
        return true;
      };
    }

    return () -> {
      FileViewProvider viewProvider = file.getViewProvider();
      if (!task.isStillValid() || documentManager.getCachedViewProvider(document) != viewProvider) {
        return false; // optimistic locking failed
      }

      if (!ApplicationManager.getApplication().isWriteAccessAllowed() && documentManager.isEventSystemEnabled(document)) {
        VirtualFile vFile = viewProvider.getVirtualFile();
        LOG.error("Write action expected" +
                    "; document=" + document +
                    "; file=" + file + " of " + file.getClass() +
                    "; file.valid=" + file.isValid() +
                    "; file.eventSystemEnabled=" + viewProvider.isEventSystemEnabled() +
                    "; viewProvider=" + viewProvider + " of " + viewProvider.getClass() +
                    "; language=" + file.getLanguage() +
                    "; vFile=" + vFile + " of " + vFile.getClass() +
                    "; free-threaded=" + AbstractFileViewProvider.isFreeThreaded(viewProvider));
      }

      diffLog.doActualPsiChange(file);

      assertAfterCommit(document, file, (FileElement)oldFileNode);

      return true;
    };
  }

  @RequiredReadAction
  private static void assertAfterCommit(@Nonnull Document document, @Nonnull final PsiFile file, @Nonnull ASTNode oldFileNode) {
    if (oldFileNode.getTextLength() != document.getTextLength()) {
      final String documentText = document.getText();
      String fileText = file.getText();
      boolean sameText = Comparing.equal(fileText, documentText);
      String errorMessage = "commitDocument() left PSI inconsistent: " + DebugUtil.diagnosePsiDocumentInconsistency(file, document) +
        "; node.length=" + oldFileNode.getTextLength() +
        "; doc.text" + (sameText ? "==" : "!=") + "file.text" +
        "; file name:" + file.getName() +
        "; type:" + file.getFileType() +
        "; lang:" + file.getLanguage();
      PluginExceptionUtil.logPluginError(LOG, errorMessage, null, file.getLanguage().getClass());

      file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
      try {
        BlockSupport blockSupport = BlockSupport.getInstance(file.getProject());
        final DiffLog diffLog = blockSupport.reparseRange(file, file.getNode(), new TextRange(0, documentText.length()), documentText,
                                                          new StandardProgressIndicatorBase(),
                                                          oldFileNode.getText());
        diffLog.doActualPsiChange(file);

        if (oldFileNode.getTextLength() != document.getTextLength()) {
          PluginExceptionUtil.logPluginError(LOG, "PSI is broken beyond repair in: " + file, null, file.getLanguage().getClass());
        }
      }
      finally {
        file.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, null);
      }
    }
  }

}
