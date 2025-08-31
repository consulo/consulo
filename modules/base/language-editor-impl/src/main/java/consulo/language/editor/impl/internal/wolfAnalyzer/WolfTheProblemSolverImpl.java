// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.wolfAnalyzer;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.internal.highlight.GeneralHighlightingPass;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoHolder;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.wolfAnalyzer.Problem;
import consulo.language.editor.wolfAnalyzer.WolfFileProblemFilter;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.event.PsiTreeChangeListener;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * @author cdr
 */
@Singleton
@ServiceImpl
public class WolfTheProblemSolverImpl extends WolfTheProblemSolver {
  private final Map<VirtualFile, ProblemFileInfo> myProblems = new HashMap<>(); // guarded by myProblems
  private final Collection<VirtualFile> myCheckingQueue = new HashSet<>(10);

  private final Project myProject;

  private void doRemove(@Nonnull VirtualFile problemFile) {
    ProblemFileInfo old;
    synchronized (myProblems) {
      old = myProblems.remove(problemFile);
    }
    synchronized (myCheckingQueue) {
      myCheckingQueue.remove(problemFile);
    }
    if (old != null) {
      // firing outside lock
      myProject.getMessageBus().syncPublisher(consulo.language.editor.wolfAnalyzer.ProblemListener.class).problemsDisappeared(problemFile);
    }
  }

  private static class ProblemFileInfo {
    private final Collection<Problem> problems = new HashSet<>();
    private boolean hasSyntaxErrors;

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ProblemFileInfo that = (ProblemFileInfo)o;

      return hasSyntaxErrors == that.hasSyntaxErrors && problems.equals(that.problems);
    }

    @Override
    public int hashCode() {
      int result = problems.hashCode();
      result = 31 * result + (hasSyntaxErrors ? 1 : 0);
      return result;
    }
  }

  @Inject
  WolfTheProblemSolverImpl(@Nonnull Project project, @Nonnull PsiManager psiManager, @Nonnull FileStatusManager fileStatusManager) {
    myProject = project;
    if (project.isDefault()) {
      return;
    }
    PsiTreeChangeListener changeListener = new PsiTreeChangeAdapter() {
      @Override
      public void childAdded(@Nonnull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childMoved(@Nonnull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
        clearSyntaxErrorFlag(event);
      }
    };
    psiManager.addPsiTreeChangeListener(changeListener);
    project.getMessageBus().connect().subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        boolean dirChanged = false;
        Set<VirtualFile> toRemove = new HashSet<>();
        for (VFileEvent event : events) {
          if (event instanceof VFileDeleteEvent || event instanceof VFileMoveEvent) {
            VirtualFile file = event.getFile();
            if (file.isDirectory()) {
              dirChanged = true;
            }
            else {
              toRemove.add(file);
            }
          }
        }
        if (dirChanged) {
          clearInvalidFiles();
        }
        for (VirtualFile file : toRemove) {
          doRemove(file);
        }
      }
    });

    fileStatusManager.addFileStatusListener(new FileStatusListener() {
      @Override
      public void fileStatusesChanged() {
        clearInvalidFiles();
      }

      @Override
      public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
        fileStatusesChanged();
      }
    });
  }

  private void clearInvalidFiles() {
    VirtualFile[] files;
    synchronized (myProblems) {
      files = VirtualFileUtil.toVirtualFileArray(myProblems.keySet());
    }
    for (VirtualFile problemFile : files) {
      if (!problemFile.isValid() || !isToBeHighlighted(problemFile)) {
        doRemove(problemFile);
      }
    }
  }

  private void clearSyntaxErrorFlag(@Nonnull PsiTreeChangeEvent event) {
    PsiFile file = event.getFile();
    if (file == null) return;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return;
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(virtualFile);
      if (info != null) {
        info.hasSyntaxErrors = false;
      }
    }
  }

  public void startCheckingIfVincentSolvedProblemsYet(@Nonnull ProgressIndicator progress, @Nonnull ProgressableTextEditorHighlightingPass pass) throws ProcessCanceledException {
    if (!myProject.isOpen()) return;

    List<VirtualFile> files;
    synchronized (myCheckingQueue) {
      files = new ArrayList<>(myCheckingQueue);
    }
    // (rough approx number of PSI elements = file length/2) * (visitor count = 2 usually)
    long progressLimit = files.stream().filter(VirtualFile::isValid).mapToLong(VirtualFile::getLength).sum();
    pass.setProgressLimit(progressLimit);
    for (VirtualFile virtualFile : files) {
      progress.checkCanceled();
      if (virtualFile == null) break;
      if (!virtualFile.isValid() || orderVincentToCleanTheCar(virtualFile, progress)) {
        doRemove(virtualFile);
      }
      if (virtualFile.isValid()) {
        pass.advanceProgress(virtualFile.getLength());
      }
    }
  }

  // returns true if car has been cleaned
  private boolean orderVincentToCleanTheCar(@Nonnull VirtualFile file, @Nonnull ProgressIndicator progressIndicator) throws ProcessCanceledException {
    if (!isToBeHighlighted(file)) {
      clearProblems(file);
      return true; // file is going to be red waved no more
    }
    if (hasSyntaxErrors(file)) {
      // optimization: it's no use anyway to try clean the file with syntax errors, only changing the file itself can help
      return false;
    }
    if (myProject.isDisposed()) return false;
    if (willBeHighlightedAnyway(file)) return false;
    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) return false;
    final Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return false;

    final AtomicReference<HighlightInfoImpl> error = new AtomicReference<>();
    final AtomicBoolean hasErrorElement = new AtomicBoolean();
    try {
      GeneralHighlightingPass pass =
              new GeneralHighlightingPass(myProject, psiFile, document, 0, document.getTextLength(), false, new ProperTextRange(0, document.getTextLength()), null, HighlightInfoProcessor.getEmpty()) {
                @Override
                protected HighlightInfoHolder createInfoHolder(@Nonnull final PsiFile file) {
                  return new HighlightInfoHolder(file, List.of()) {
                    @Override
                    public boolean add(@Nullable HighlightInfo info) {
                      if (info != null && info.getSeverity() == HighlightSeverity.ERROR) {
                        error.set((HighlightInfoImpl)info);
                        hasErrorElement.set(myHasErrorElement);
                        throw new ProcessCanceledException();
                      }
                      return super.add(info);
                    }
                  };
                }
              };
      pass.collectInformation(progressIndicator);
    }
    catch (ProcessCanceledException e) {
      if (error.get() != null) {
        ProblemImpl problem = new ProblemImpl(file, error.get(), hasErrorElement.get());
        reportProblems(file, Collections.singleton(problem));
      }
      return false;
    }
    clearProblems(file);
    return true;
  }

  @Override
  public boolean hasSyntaxErrors(VirtualFile file) {
    synchronized (myProblems) {
      ProblemFileInfo info = myProblems.get(file);
      return info != null && info.hasSyntaxErrors;
    }
  }

  private boolean willBeHighlightedAnyway(VirtualFile file) {
    // opened in some editor, and hence will be highlighted automatically sometime later
    FileEditor[] selectedEditors = FileEditorManager.getInstance(myProject).getSelectedEditors();
    for (FileEditor editor : selectedEditors) {
      if (!(editor instanceof TextEditor)) continue;
      Document document = ((TextEditor)editor).getEditor().getDocument();
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getCachedPsiFile(document);
      if (psiFile == null) continue;
      if (Comparing.equal(file, psiFile.getVirtualFile())) return true;
    }
    return false;
  }

  @Override
  public boolean hasProblemFilesBeneath(@Nonnull Predicate<VirtualFile> condition) {
    if (!myProject.isOpen()) return false;
    synchronized (myProblems) {
      if (!myProblems.isEmpty()) {
        for (VirtualFile problemFile : myProblems.keySet()) {
          if (problemFile.isValid() && condition.test(problemFile)) return true;
        }
      }
      return false;
    }
  }

  @Override
  public boolean hasProblemFilesBeneath(@Nonnull Module scope) {
    return hasProblemFilesBeneath(virtualFile -> ModuleUtilCore.moduleContainsFile(scope, virtualFile, false));
  }

  @Override
  public void addProblemListener(@Nonnull WolfTheProblemSolver.ProblemListener listener, @Nonnull Disposable parentDisposable) {
    myProject.getMessageBus().connect(parentDisposable).subscribe(consulo.language.editor.wolfAnalyzer.ProblemListener.class, listener);
  }

  @Override
  public void queue(VirtualFile suspiciousFile) {
    if (!isToBeHighlighted(suspiciousFile)) return;
    doQueue(suspiciousFile);
  }

  private void doQueue(@Nonnull VirtualFile suspiciousFile) {
    synchronized (myCheckingQueue) {
      myCheckingQueue.add(suspiciousFile);
    }
  }

  @Override
  public boolean isProblemFile(VirtualFile virtualFile) {
    synchronized (myProblems) {
      return myProblems.containsKey(virtualFile);
    }
  }

  private boolean isToBeHighlighted(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null) return false;

    for (WolfFileProblemFilter filter : myProject.getExtensionPoint(WolfFileProblemFilter.class).getExtensionList()) {
      ProgressManager.checkCanceled();
      if (filter.isToBeHighlighted(virtualFile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void weHaveGotProblems(@Nonnull VirtualFile virtualFile, @Nonnull List<Problem> problems) {
    if (problems.isEmpty()) return;
    if (!isToBeHighlighted(virtualFile)) return;
    weHaveGotNonIgnorableProblems(virtualFile, problems);
  }

  @Override
  public void weHaveGotNonIgnorableProblems(@Nonnull VirtualFile virtualFile, @Nonnull List<Problem> problems) {
    if (problems.isEmpty()) return;
    boolean fireListener = false;
    synchronized (myProblems) {
      ProblemFileInfo storedProblems = myProblems.get(virtualFile);
      if (storedProblems == null) {
        storedProblems = new ProblemFileInfo();

        myProblems.put(virtualFile, storedProblems);
        fireListener = true;
      }
      storedProblems.problems.addAll(problems);
    }
    doQueue(virtualFile);
    if (fireListener) {
      myProject.getMessageBus().syncPublisher(consulo.language.editor.wolfAnalyzer.ProblemListener.class).problemsAppeared(virtualFile);
    }
  }

  @Override
  public void clearProblems(@Nonnull VirtualFile virtualFile) {
    doRemove(virtualFile);
  }

  @Override
  public Problem convertToProblem(@Nullable VirtualFile virtualFile, int line, int column, @Nonnull String[] message) {
    if (virtualFile == null || virtualFile.isDirectory() || virtualFile.getFileType().isBinary()) return null;
    HighlightInfo info = ReadAction.compute(() -> {
      TextRange textRange = getTextRange(virtualFile, line, column);
      String description = StringUtil.join(message, "\n");
      return HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(textRange).descriptionAndTooltip(description).create();
    });
    if (info == null) return null;
    return new ProblemImpl(virtualFile, (HighlightInfoImpl)info, false);
  }

  @Override
  public void reportProblems(@Nonnull VirtualFile file, @Nonnull Collection<Problem> problems) {
    if (problems.isEmpty()) {
      clearProblems(file);
      return;
    }
    if (!isToBeHighlighted(file)) return;
    boolean hasProblemsBefore;
    boolean fireChanged;
    synchronized (myProblems) {
      ProblemFileInfo oldInfo = myProblems.remove(file);
      hasProblemsBefore = oldInfo != null;
      ProblemFileInfo newInfo = new ProblemFileInfo();
      myProblems.put(file, newInfo);
      for (Problem problem : problems) {
        newInfo.problems.add(problem);
        newInfo.hasSyntaxErrors |= ((ProblemImpl)problem).isSyntaxOnly();
      }
      fireChanged = hasProblemsBefore && !oldInfo.equals(newInfo);
    }
    doQueue(file);
    if (!hasProblemsBefore) {
      myProject.getMessageBus().syncPublisher(consulo.language.editor.wolfAnalyzer.ProblemListener.class).problemsAppeared(file);
    }
    else if (fireChanged) {
      myProject.getMessageBus().syncPublisher(consulo.language.editor.wolfAnalyzer.ProblemListener.class).problemsChanged(file);
    }
  }

  @Nonnull
  private static TextRange getTextRange(@Nonnull VirtualFile virtualFile, int line, int column) {
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (line > document.getLineCount()) line = document.getLineCount();
    line = line <= 0 ? 0 : line - 1;
    int offset = document.getLineStartOffset(line) + (column <= 0 ? 0 : column - 1);
    return new TextRange(offset, offset);
  }
}
