// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.actions;

import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.SelectionModel;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ui.ex.awt.MessagesEx;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ide.impl.idea.util.SequentialTask;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiBundle;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.application.localize.ApplicationLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public abstract class AbstractLayoutCodeProcessor {
  public static final NotificationGroup ReformatChangedTextTooBigNotificationGroup =
          NotificationGroup.balloonGroup("reformat.changed.text.file.too.big.notification.groupId", ApplicationLocalize.reformatChangedTextFileTooBigNotificationGroupid());

  private static final Logger LOG = Logger.getInstance(AbstractLayoutCodeProcessor.class);

  @Nonnull
  protected final Project myProject;
  private final Module myModule;

  private PsiDirectory myDirectory;
  private PsiFile myFile;
  private List<PsiFile> myFiles;
  private boolean myIncludeSubdirs;

  private final String myProgressText;
  private final String myCommandName;
  private Runnable myPostRunnable;
  private boolean myProcessChangedTextOnly;

  protected AbstractLayoutCodeProcessor myPreviousCodeProcessor;
  private List<VirtualFileFilter> myFilters = new ArrayList<>();

  private LayoutCodeInfoCollector myInfoCollector;

  protected AbstractLayoutCodeProcessor(@Nonnull Project project, String commandName, String progressText, boolean processChangedTextOnly) {
    this(project, (Module)null, commandName, progressText, processChangedTextOnly);
  }

  protected AbstractLayoutCodeProcessor(@Nonnull AbstractLayoutCodeProcessor previous, @Nonnull String commandName, @Nonnull String progressText) {
    myProject = previous.myProject;
    myModule = previous.myModule;
    myDirectory = previous.myDirectory;
    myFile = previous.myFile;
    myFiles = previous.myFiles;
    myIncludeSubdirs = previous.myIncludeSubdirs;
    myProcessChangedTextOnly = previous.myProcessChangedTextOnly;

    myPostRunnable = null;
    myProgressText = progressText;
    myCommandName = commandName;
    myPreviousCodeProcessor = previous;
    myFilters = previous.myFilters;
    myInfoCollector = previous.myInfoCollector;
  }

  protected AbstractLayoutCodeProcessor(@Nonnull Project project, @Nullable Module module, String commandName, String progressText, boolean processChangedTextOnly) {
    myProject = project;
    myModule = module;
    myDirectory = null;
    myIncludeSubdirs = true;
    myCommandName = commandName;
    myProgressText = progressText;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@Nonnull Project project, PsiDirectory directory, boolean includeSubdirs, String progressText, String commandName, boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myDirectory = directory;
    myIncludeSubdirs = includeSubdirs;
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@Nonnull Project project, PsiFile file, String progressText, String commandName, boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myFile = file;
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = null;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  protected AbstractLayoutCodeProcessor(@Nonnull Project project, PsiFile[] files, String progressText, String commandName, @Nullable Runnable postRunnable, boolean processChangedTextOnly) {
    myProject = project;
    myModule = null;
    myFiles = ContainerUtil.filter(files, AbstractLayoutCodeProcessor::canBeFormatted);
    myProgressText = progressText;
    myCommandName = commandName;
    myPostRunnable = postRunnable;
    myProcessChangedTextOnly = processChangedTextOnly;
  }

  public void setPostRunnable(Runnable postRunnable) {
    myPostRunnable = postRunnable;
  }

  @Nullable
  private FutureTask<Boolean> getPreviousProcessorTask(@Nonnull PsiFile file, boolean processChangedTextOnly) {
    return myPreviousCodeProcessor != null ? myPreviousCodeProcessor.preprocessFile(file, processChangedTextOnly) : null;
  }

  public void setCollectInfo(boolean isCollectInfo) {
    myInfoCollector = isCollectInfo ? new LayoutCodeInfoCollector() : null;

    AbstractLayoutCodeProcessor current = this;
    while (current.myPreviousCodeProcessor != null) {
      current = current.myPreviousCodeProcessor;
      current.myInfoCollector = myInfoCollector;
    }
  }

  public void addFileFilter(@Nonnull VirtualFileFilter filter) {
    myFilters.add(filter);
  }

  void setProcessChangedTextOnly(boolean value) {
    myProcessChangedTextOnly = value;
  }

  /**
   * Ensures that given file is ready to reformatting and prepares it if necessary.
   *
   * @param file                   file to process
   * @param processChangedTextOnly flag that defines is only the changed text (in terms of VCS change) should be processed
   * @return task that triggers formatting of the given file. Returns value of that task indicates whether formatting
   * is finished correctly or not (exception occurred, user cancelled formatting etc)
   * @throws IncorrectOperationException if unexpected exception occurred during formatting
   */
  @Nonnull
  protected abstract FutureTask<Boolean> prepareTask(@Nonnull PsiFile file, boolean processChangedTextOnly) throws IncorrectOperationException;

  public FutureTask<Boolean> preprocessFile(@Nonnull PsiFile file, boolean processChangedTextOnly) throws IncorrectOperationException {
    final FutureTask<Boolean> previousTask = getPreviousProcessorTask(file, processChangedTextOnly);
    final FutureTask<Boolean> currentTask = prepareTask(file, processChangedTextOnly);

    return new FutureTask<>(() -> {
      try {
        if (previousTask != null) {
          previousTask.run();
          if (!previousTask.get() || previousTask.isCancelled()) return false;
        }

        ApplicationManager.getApplication().runWriteAction(currentTask);

        return currentTask.get() && !currentTask.isCancelled();
      }
      catch (ExecutionException e) {
        ExceptionUtil.rethrowUnchecked(e.getCause());
        throw e;
      }
    });
  }

  public void run() {
    if (myFile != null) {
      runProcessFile(myFile);
      return;
    }

    runProcessFiles();
  }

  @Nonnull
  private FileRecursiveIterator build() {
    if (myFiles != null) {
      return new FileRecursiveIterator(myProject, myFiles);
    }
    if (myProcessChangedTextOnly) {
      return buildChangedFilesIterator();
    }
    if (myDirectory != null) {
      return new FileRecursiveIterator(myDirectory);
    }
    if (myModule != null) {
      return new FileRecursiveIterator(myModule);
    }
    return new FileRecursiveIterator(myProject);
  }

  @Nonnull
  private FileRecursiveIterator buildChangedFilesIterator() {
    List<PsiFile> files = getChangedFilesFromContext();
    return new FileRecursiveIterator(myProject, files);
  }

  @Nonnull
  private List<PsiFile> getChangedFilesFromContext() {
    List<PsiDirectory> dirs = getAllSearchableDirsFromContext();
    return FormatChangedTextUtil.getChangedFilesFromDirs(myProject, dirs);
  }

  private List<PsiDirectory> getAllSearchableDirsFromContext() {
    List<PsiDirectory> dirs = new ArrayList<>();
    if (myDirectory != null) {
      dirs.add(myDirectory);
    }
    else if (myModule != null) {
      List<PsiDirectory> allModuleDirs = FileRecursiveIterator.collectModuleDirectories(myModule);
      dirs.addAll(allModuleDirs);
    }
    else {
      List<PsiDirectory> allProjectDirs = FileRecursiveIterator.collectProjectDirectories(myProject);
      dirs.addAll(allProjectDirs);
    }
    return dirs;
  }

  private void runProcessFile(@Nonnull final PsiFile file) {
    assert file.isValid() : "Invalid " + file.getLanguage() + " PSI file " + file.getName();

    Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);

    if (document == null) {
      return;
    }

    if (!FileDocumentManager.getInstance().requestWriting(document, myProject)) {
      Messages.showMessageDialog(myProject, PsiBundle.message("cannot.modify.a.read.only.file", file.getName()), CodeInsightBundle.message("error.dialog.readonly.file.title"),
                                 Messages.getErrorIcon());
      return;
    }

    final Ref<FutureTask<Boolean>> writeActionRunnable = new Ref<>();
    Runnable readAction = () -> {
      if (!file.isValid() || !checkFileWritable(file)) return;
      try {
        FutureTask<Boolean> writeTask = preprocessFile(file, myProcessChangedTextOnly);
        writeActionRunnable.set(writeTask);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    };
    Runnable writeAction = () -> {
      if (writeActionRunnable.isNull()) return;
      FutureTask<Boolean> task = writeActionRunnable.get();
      task.run();
      try {
        task.get();
      }
      catch (CancellationException ignored) {
      }
      catch (Exception e) {
        Throwable cause = e.getCause();
        if (cause != null) {
          if (cause instanceof IndexNotReadyException) {
            throw (IndexNotReadyException)e.getCause();
          }
          LOG.error(getClass().getSimpleName() + " failure, see the stack trace", cause);
        }
        else {
          LOG.error(e);
        }
      }
    };
    runLayoutCodeProcess(readAction, writeAction);
  }

  private boolean checkFileWritable(final PsiFile file) {
    if (!file.isWritable()) {
      MessagesEx.fileIsReadOnly(myProject, file.getVirtualFile()).setTitle(CodeInsightBundle.message("error.dialog.readonly.file.title")).showLater();
      return false;
    }
    else {
      return true;
    }
  }

  private void runProcessFiles() {
    boolean isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      indicator.setIndeterminate(false);
      ReformatFilesTask task = new ReformatFilesTask(indicator);
      return task.process();
    }, myCommandName, true, myProject);

    if (isSuccess && myPostRunnable != null) {
      myPostRunnable.run();
    }
  }

  private static boolean canBeFormatted(PsiFile file) {
    if (!file.isValid()) return false;
    if (FormattingModelBuilder.forContext(file) == null) {
      return false;
    }
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return true;

    if (ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile)) return false;

    return !GeneratedSourcesFilter.isGenerated(file.getProject(), virtualFile);
  }

  private void runLayoutCodeProcess(final Runnable readAction, final Runnable writeAction) {
    final ProgressWindow progressWindow = new ProgressWindow(true, myProject);
    progressWindow.setTitle(myCommandName);
    progressWindow.setText(myProgressText);

    final IdeaModalityState modalityState = IdeaModalityState.current();

    final Runnable process = () -> ApplicationManager.getApplication().runReadAction(readAction);

    Runnable runnable = () -> {
      try {
        ProgressManager.getInstance().runProcess(process, progressWindow);
      }
      catch (ProcessCanceledException e) {
        return;
      }
      catch (IndexNotReadyException e) {
        LOG.warn(e);
        return;
      }

      final Runnable writeRunnable = () -> CommandProcessor.getInstance().executeCommand(myProject, () -> {
        try {
          writeAction.run();

          if (myPostRunnable != null) {
            ApplicationManager.getApplication().invokeLater(myPostRunnable);
          }
        }
        catch (IndexNotReadyException e) {
          LOG.warn(e);
        }
      }, myCommandName, null);

      if (ApplicationManager.getApplication().isUnitTestMode()) {
        writeRunnable.run();
      }
      else {
        ApplicationManager.getApplication().invokeLater(writeRunnable, modalityState, myProject.getDisposed());
      }
    };

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runnable.run();
    }
    else {
      ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }
  }

  public void runWithoutProgress() throws IncorrectOperationException {
    final Runnable runnable = preprocessFile(myFile, myProcessChangedTextOnly);
    runnable.run();
  }

  private List<AbstractLayoutCodeProcessor> getAllProcessors() {
    AbstractLayoutCodeProcessor current = this;
    List<AbstractLayoutCodeProcessor> all = new ArrayList<>();
    while (current != null) {
      all.add(current);
      current = current.myPreviousCodeProcessor;
    }
    Collections.reverse(all);
    return all;
  }

  private class ReformatFilesTask implements SequentialTask {
    private final List<AbstractLayoutCodeProcessor> myProcessors;

    private final FileRecursiveIterator myFileTreeIterator;
    private final FileRecursiveIterator myCountingIterator;

    private final ProgressIndicator myProgressIndicator;

    private int myTotalFiles;
    private int myFilesProcessed;
    private boolean myStopFormatting;
    private PsiFile next;

    ReformatFilesTask(@Nonnull ProgressIndicator indicator) {
      myFileTreeIterator = ReadAction.compute(() -> build());
      myCountingIterator = ReadAction.compute(() -> build());
      myProcessors = getAllProcessors();
      myProgressIndicator = indicator;
    }

    @Override
    public boolean isDone() {
      return myStopFormatting;
    }

    private void countingIteration() {
      myTotalFiles++;
    }

    @Override
    public boolean iteration() {
      if (myStopFormatting) {
        return true;
      }

      updateIndicatorFraction(myFilesProcessed);

      if (next != null) {
        PsiFile file = next;
        myFilesProcessed++;

        if (shouldProcessFile(file)) {
          updateIndicatorText(ApplicationBundle.message("bulk.reformat.process.progress.text"), getPresentablePath(file));
          DumbService.getInstance(myProject).withAlternativeResolveEnabled(() -> performFileProcessing(file));
        }
      }

      return true;
    }

    private Boolean shouldProcessFile(PsiFile file) {
      return ReadAction.compute(() -> file.isWritable() && canBeFormatted(file) && acceptedByFilters(file));
    }

    private void performFileProcessing(@Nonnull PsiFile file) {
      for (AbstractLayoutCodeProcessor processor : myProcessors) {
        FutureTask<Boolean> writeTask = ReadAction.compute(() -> processor.prepareTask(file, myProcessChangedTextOnly));

        ProgressIndicatorProvider.checkCanceled();

        ApplicationManager.getApplication().invokeAndWait(() -> WriteCommandAction.runWriteCommandAction(myProject, myCommandName, null, writeTask));

        checkStop(writeTask, file);
      }
    }

    private void checkStop(FutureTask<Boolean> task, PsiFile file) {
      try {
        if (!task.get() || task.isCancelled()) {
          myStopFormatting = true;
        }
      }
      catch (InterruptedException | ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IndexNotReadyException) {
          LOG.warn(cause);
          return;
        }
        LOG.error("Got unexpected exception during formatting " + file, e);
      }
    }

    private void updateIndicatorText(@Nonnull String upperLabel, @Nonnull String downLabel) {
      myProgressIndicator.setText(upperLabel);
      myProgressIndicator.setText2(downLabel);
    }

    private String getPresentablePath(@Nonnull PsiFile file) {
      VirtualFile vFile = file.getVirtualFile();
      return vFile != null ? ProjectUtil.calcRelativeToProjectPath(vFile, myProject) : file.getName();
    }

    private void updateIndicatorFraction(int processed) {
      myProgressIndicator.setFraction((double)processed / myTotalFiles);
    }

    @Override
    public void stop() {
      myStopFormatting = true;
    }

    public boolean process() {
      myCountingIterator.processAll(file -> {
        updateIndicatorText(ApplicationBundle.message("bulk.reformat.prepare.progress.text"), "");
        countingIteration();
        return !isDone();
      });

      return myFileTreeIterator.processAll(file -> {
        next = file;
        iteration();
        return !isDone();
      });
    }
  }

  private boolean acceptedByFilters(@Nonnull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) {
      return false;
    }

    for (VirtualFileFilter filter : myFilters) {
      if (!filter.accept(file.getVirtualFile())) {
        return false;
      }
    }

    return true;
  }

  static List<TextRange> getSelectedRanges(@Nonnull SelectionModel selectionModel) {
    final List<TextRange> ranges = new SmartList<>();
    if (selectionModel.hasSelection()) {
      TextRange range = TextRange.create(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
      ranges.add(range);
    }
    return ranges;
  }

  void handleFileTooBigException(Logger logger, FilesTooBigForDiffException e, @Nonnull PsiFile file) {
    logger.info("Error while calculating changed ranges for: " + file.getVirtualFile(), e);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      Notification notification = new Notification(ReformatChangedTextTooBigNotificationGroup, ApplicationBundle.message("reformat.changed.text.file.too.big.notification.title"),
                                                   ApplicationBundle.message("reformat.changed.text.file.too.big.notification.text", file.getName()), NotificationType.INFORMATION);
      notification.notify(file.getProject());
    }
  }

  @Nullable
  public LayoutCodeInfoCollector getInfoCollector() {
    return myInfoCollector;
  }
}