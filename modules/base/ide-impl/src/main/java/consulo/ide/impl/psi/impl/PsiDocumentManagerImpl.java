// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.psi.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.EditorFactory;
import consulo.component.messagebus.MessageBusConnection;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.FileDocumentManagerListener;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.impl.event.EditorEventMulticasterImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileDocumentManagerImpl;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.fileEditor.util.FileContentUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.psi.impl.source.PostprocessReformattingAspectImpl;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.PostprocessReformattingAspect;
import consulo.language.file.FileViewProvider;
import consulo.document.DocumentWindow;
import consulo.language.impl.internal.pom.PomAspectGuard;
import consulo.language.impl.internal.psi.BooleanRunnable;
import consulo.language.impl.internal.psi.DocumentCommitProcessor;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageManagerImpl;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.impl.internal.ProjectImpl;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import java.util.*;

//todo listen & notifyListeners readonly events?
@Singleton
@ServiceImpl
public final class PsiDocumentManagerImpl extends PsiDocumentManagerBase {
  private static final Logger LOG = Logger.getInstance(PsiDocumentManagerImpl.class);

  private final boolean myUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();

  @Inject
  public PsiDocumentManagerImpl(@Nonnull Project project, @Nonnull DocumentCommitProcessor documentCommitProcessor) {
    super(project, documentCommitProcessor);

    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, this);
    ((EditorEventMulticasterImpl)EditorFactory.getInstance().getEventMulticaster()).addPrioritizedDocumentListener(new PriorityEventCollector(), this);
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
      @Override
      public void fileContentLoaded(@Nonnull final VirtualFile virtualFile, @Nonnull Document document) {
        PsiFile psiFile = ReadAction.compute(() -> myProject.isDisposed() || !virtualFile.isValid() ? null : getCachedPsiFile(virtualFile));
        fireDocumentCreated(document, psiFile);
      }
    });
  }

  @Nullable
  @Override
  public PsiFile getPsiFile(@Nonnull Document document) {
    final PsiFile psiFile = super.getPsiFile(document);
    if (myUnitTestMode) {
      final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
      if (virtualFile != null && virtualFile.isValid()) {
        Collection<Project> projects = ProjectLocator.getInstance().getProjectsForFile(virtualFile);
        if (!projects.isEmpty() && !projects.contains(myProject)) {
          LOG.error("Trying to get PSI for an alien project. VirtualFile=" + virtualFile + ";\n myProject=" + myProject + ";\n projects returned: " + projects);
        }
      }
    }
    return psiFile;
  }

  @Override
  public void documentChanged(@Nonnull DocumentEvent event) {
    super.documentChanged(event);
    // optimisation: avoid documents piling up during batch processing
    if (isUncommited(event.getDocument()) && FileDocumentManagerImpl.areTooManyDocumentsInTheQueue(myUncommittedDocuments)) {
      if (myUnitTestMode) {
        myStopTrackingDocuments = true;
        try {
          LOG.error("Too many uncommitted documents for " +
                    myProject +
                    "(" +
                    myUncommittedDocuments.size() +
                    ")" +
                    ":\n" +
                    StringUtil.join(myUncommittedDocuments, "\n") +
                    (myProject instanceof ProjectImpl ? "\n\n Project creation trace: " + ((ProjectImpl)myProject).getCreationTrace() : ""));
        }
        finally {
          //noinspection TestOnlyProblems
          clearUncommittedDocuments();
        }
      }
      // must not commit during document save
      if (PomAspectGuard.isAllowPsiModification()
          // it can happen that document(forUseInNonAWTThread=true) outside write action caused this
          && ApplicationManager.getApplication().isWriteAccessAllowed()) {
        // commit one document to avoid OOME
        for (Document document : myUncommittedDocuments) {
          if (document != event.getDocument()) {
            doCommitWithoutReparse(document);
            break;
          }
        }
      }
    }
  }

  @Override
  protected void beforeDocumentChangeOnUnlockedDocument(@Nonnull final FileViewProvider viewProvider) {
    ((PostprocessReformattingAspectImpl)PostprocessReformattingAspect.getInstance(myProject)).assertDocumentChangeIsAllowed(viewProvider);
    super.beforeDocumentChangeOnUnlockedDocument(viewProvider);
  }

  @Override
  protected boolean finishCommitInWriteAction(@Nonnull Document document,
                                              @Nonnull List<? extends BooleanRunnable> finishProcessors,
                                              @Nonnull List<? extends BooleanRunnable> reparseInjectedProcessors,
                                              boolean synchronously,
                                              boolean forceNoPsiCommit) {
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) { // can be false for non-physical PSI
      InjectedLanguageManagerImpl.disposeInvalidEditors();
    }
    return super.finishCommitInWriteAction(document, finishProcessors, reparseInjectedProcessors, synchronously, forceNoPsiCommit);
  }

  @Override
  public boolean isDocumentBlockedByPsi(@Nonnull Document doc) {
    final FileViewProvider viewProvider = getCachedViewProvider(doc);
    return viewProvider != null && PostprocessReformattingAspect.getInstance(myProject).isViewProviderLocked(viewProvider);
  }

  @Override
  public void doPostponedOperationsAndUnblockDocument(@Nonnull Document doc) {
    if (doc instanceof DocumentWindow) doc = ((DocumentWindow)doc).getDelegate();
    final PostprocessReformattingAspectImpl component = (PostprocessReformattingAspectImpl)PostprocessReformattingAspect.getInstance(myProject);
    final FileViewProvider viewProvider = getCachedViewProvider(doc);
    if (viewProvider != null && component != null) component.doPostponedFormatting(viewProvider);
  }

  @Nonnull
  @Override
  public List<BooleanRunnable> reparseChangedInjectedFragments(@Nonnull Document hostDocument,
                                                               @Nonnull PsiFile hostPsiFile,
                                                               @Nonnull TextRange hostChangedRange,
                                                               @Nonnull ProgressIndicator indicator,
                                                               @Nonnull ASTNode oldRoot,
                                                               @Nonnull ASTNode newRoot) {
    List<DocumentWindow> changedInjected = InjectedLanguageManager.getInstance(myProject).getCachedInjectedDocumentsInRange(hostPsiFile, hostChangedRange);
    if (changedInjected.isEmpty()) return Collections.emptyList();
    FileViewProvider hostViewProvider = hostPsiFile.getViewProvider();
    List<DocumentWindow> fromLast = new ArrayList<>(changedInjected);
    // make sure modifications do not ruin all document offsets after
    fromLast.sort(Collections.reverseOrder(Comparator.comparingInt(doc -> ArrayUtil.getLastElement(doc.getHostRanges()).getEndOffset())));
    List<BooleanRunnable> result = new ArrayList<>(changedInjected.size());
    for (DocumentWindow document : fromLast) {
      Segment[] ranges = document.getHostRanges();
      if (ranges.length != 0) {
        // host document change has left something valid in this document window place. Try to reparse.
        PsiFile injectedPsiFile = getCachedPsiFile(document);
        if (injectedPsiFile == null || !injectedPsiFile.isValid()) continue;

        BooleanRunnable runnable = InjectedLanguageUtil.reparse(injectedPsiFile, document, hostPsiFile, hostDocument, hostViewProvider, indicator, oldRoot, newRoot, this);
        ContainerUtil.addIfNotNull(result, runnable);
      }
    }

    return result;
  }

  @NonNls
  @Override
  public String toString() {
    return super.toString() + " for the project " + myProject + ".";
  }

  @Override
  public void reparseFiles(@Nonnull Collection<? extends VirtualFile> files, boolean includeOpenFiles) {
    FileContentUtil.reparseFiles(myProject, files, includeOpenFiles);
  }

  @Nonnull
  @Override
  protected DocumentWindow freezeWindow(@Nonnull DocumentWindow document) {
    return InjectedLanguageManager.getInstance(myProject).freezeWindow(document);
  }

  @Override
  public void associatePsi(@Nonnull Document document, @Nullable PsiFile file) {
    if (file != null) {
      VirtualFile vFile = file.getViewProvider().getVirtualFile();
      Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(vFile);
      if (cachedDocument != null && cachedDocument != document) {
        throw new IllegalStateException("Can't replace existing document");
      }

      FileDocumentManagerImpl.registerDocumentImpl(document, vFile);
    }
  }
}
