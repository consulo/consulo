/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.impl.internal.pom;

import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.util.DisposableList;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.internal.psi.*;
import consulo.language.impl.internal.psi.diff.BlockSupport;
import consulo.language.impl.internal.psi.diff.BlockSupportImpl;
import consulo.language.impl.internal.psi.diff.DiffLog;
import consulo.language.impl.internal.psi.pointer.SmartPointerManagerImpl;
import consulo.language.impl.psi.DummyHolder;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.pom.*;
import consulo.language.pom.event.PomModelEvent;
import consulo.language.pom.event.PomModelListener;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Stack;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.CompoundRuntimeException;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class PomModelImpl extends UserDataHolderBase implements PomModel {
  private final Project myProject;
  private final DisposableList<PomModelListener> myListeners = DisposableList.create();

  private final ThreadLocal<Stack<Pair<PomModelAspect, PomTransaction>>> myBlockedAspects = ThreadLocal.withInitial(Stack::new);

  @Inject
  public PomModelImpl(Project project) {
    myProject = project;
  }

  @Nonnull
  public PomAspects getCache() {
    return myProject.getExtensionPoint(PomModelAspect.class).getOrBuildCache(PomAspects.CACHE_KEY);
  }

  private List<PomModelAspect> getAllDependants(PomModelAspect aspect) {
    return getCache().getAllDependants(aspect);
  }

  @Override
  public boolean isAllowPsiModification() {
    return PomAspectGuard.isAllowPsiModification();
  }

  @Override
  public <T extends PomModelAspect> T getModelAspect(@Nonnull Class<T> aClass) {
    return getCache().getModelAspect(aClass);
  }

  @Override
  public void addModelListener(@Nonnull final PomModelListener listener, @Nonnull Disposable parentDisposable) {
    myListeners.add(listener, parentDisposable);
  }

  @Override
  public void runTransaction(@Nonnull PomTransaction transaction) throws IncorrectOperationException {
    if (!PomAspectGuard.isAllowPsiModification()) {
      throw new IncorrectOperationException("Must not modify PSI inside save listener");
    }
    final PomModelAspect aspect = transaction.getTransactionAspect();
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      startTransaction(transaction);

      Pair<PomModelAspect, PomTransaction> block = getBlockingTransaction(aspect, transaction);
      if (block != null) {
        block.getSecond().getAccumulatedEvent().beforeNestedTransaction();
      }

      List<Throwable> throwables = new ArrayList<>(0);
      DebugUtil.performPsiModification(null, () -> {
        try {
          Stack<Pair<PomModelAspect, PomTransaction>> blockedAspects = myBlockedAspects.get();
          blockedAspects.push(Pair.create(aspect, transaction));

          final PomModelEvent event;
          try {
            transaction.run();
            event = transaction.getAccumulatedEvent();
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Exception e) {
            throwables.add(e);
            return;
          }
          finally {
            blockedAspects.pop();
          }
          if (block != null) {
            block.getSecond().getAccumulatedEvent().merge(event);
            return;
          }

          { // update
            final Set<PomModelAspect> changedAspects = event.getChangedAspects();
            final Collection<PomModelAspect> dependants = new LinkedHashSet<>();
            for (final PomModelAspect pomModelAspect : changedAspects) {
              dependants.addAll(getAllDependants(pomModelAspect));
            }
            for (final PomModelAspect modelAspect : dependants) {
              if (!changedAspects.contains(modelAspect)) {
                modelAspect.update(event);
              }
            }
          }
          for (final PomModelListener listener : myListeners) {
            final Set<PomModelAspect> changedAspects = event.getChangedAspects();
            for (PomModelAspect modelAspect : changedAspects) {
              if (listener.isAspectChangeInteresting(modelAspect)) {
                listener.modelChanged(event);
                break;
              }
            }
          }
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable t) {
          throwables.add(t);
        }
        finally {
          try {
            commitTransaction(transaction);
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Throwable t) {
            throwables.add(t);
          }
          if (!throwables.isEmpty()) CompoundRuntimeException.throwIfNotEmpty(throwables);
        }
      });
    });
  }

  @Nullable
  private Pair<PomModelAspect, PomTransaction> getBlockingTransaction(final PomModelAspect aspect, PomTransaction transaction) {
    final List<PomModelAspect> allDependants = getAllDependants(aspect);
    for (final PomModelAspect pomModelAspect : allDependants) {
      Stack<Pair<PomModelAspect, PomTransaction>> blockedAspects = myBlockedAspects.get();
      ListIterator<Pair<PomModelAspect, PomTransaction>> blocksIterator = blockedAspects.listIterator(blockedAspects.size());
      while (blocksIterator.hasPrevious()) {
        final Pair<PomModelAspect, PomTransaction> pair = blocksIterator.previous();
        if (pomModelAspect == pair.getFirst() && // aspect dependence
          PsiTreeUtil.isAncestor(getContainingFileByTree(pair.getSecond().getChangeScope()),
                                 transaction.getChangeScope(),
                                 false) // same file
        ) {
          return pair;
        }
      }
    }
    return null;
  }

  private void commitTransaction(final PomTransaction transaction) {
    final PsiDocumentManagerBase manager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject);
    final PsiToDocumentSynchronizer synchronizer = manager.getSynchronizer();
    final PsiFile containingFileByTree = getContainingFileByTree(transaction.getChangeScope());
    Document document = containingFileByTree != null ? manager.getCachedDocument(containingFileByTree) : null;

    boolean isFromCommit = myProject.getApplication()
                                    .isDispatchThread() && ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject)).isCommitInProgress();
    boolean isPhysicalPsiChange = containingFileByTree != null && !isFromCommit && !synchronizer.isIgnorePsiEvents();
    if (isPhysicalPsiChange) {
      reparseParallelTrees(containingFileByTree, synchronizer);
    }

    boolean docSynced = false;
    if (document != null) {
      final int oldLength = containingFileByTree.getTextLength();
      docSynced = synchronizer.commitTransaction(document);
      if (docSynced) {
        BlockSupportImpl.sendAfterChildrenChangedEvent((PsiManagerImpl)PsiManager.getInstance(myProject),
                                                       containingFileByTree,
                                                       oldLength,
                                                       true);
      }
    }

    if (isPhysicalPsiChange && docSynced) {
      containingFileByTree.getViewProvider().contentsSynchronized();
    }

  }

  private void reparseParallelTrees(PsiFile changedFile, PsiToDocumentSynchronizer synchronizer) {
    List<PsiFile> allFiles = changedFile.getViewProvider().getAllFiles();
    if (allFiles.size() <= 1) {
      return;
    }

    CharSequence newText = changedFile.getNode().getChars();
    for (final PsiFile file : allFiles) {
      FileElement fileElement = file == changedFile ? null : ((PsiFileImpl)file).getTreeElement();
      Runnable changeAction = fileElement == null ? null : reparseFile(file, fileElement, newText);
      if (changeAction == null) continue;

      synchronizer.setIgnorePsiEvents(true);
      try {
        FormattingService.getInstance(file.getProject()).performActionWithFormatterDisabled(changeAction);
      }
      finally {
        synchronizer.setIgnorePsiEvents(false);
      }
    }
  }

  @Nullable
  private Runnable reparseFile(@Nonnull final PsiFile file, @Nonnull FileElement treeElement, @Nonnull CharSequence newText) {
    TextRange changedPsiRange = ChangedPsiRangeUtil.getChangedPsiRange(file, treeElement, newText);
    if (changedPsiRange == null) return null;

    final DiffLog log = BlockSupport.getInstance(myProject)
                                    .reparseRange(file,
                                                  treeElement,
                                                  changedPsiRange,
                                                  newText,
                                                  new EmptyProgressIndicator(),
                                                  treeElement.getText());
    return () -> runTransaction(new PomTransactionBase(file, getModelAspect(TreeAspect.class)) {
      @Override
      public PomModelEvent runInner() throws IncorrectOperationException {
        return new TreeAspectEvent(PomModelImpl.this, log.performActualPsiChange(file));
      }
    });
  }

  private void startTransaction(@Nonnull PomTransaction transaction) {
    final PsiDocumentManagerBase manager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject);
    final PsiToDocumentSynchronizer synchronizer = manager.getSynchronizer();
    final PsiElement changeScope = transaction.getChangeScope();

    final PsiFile containingFileByTree = getContainingFileByTree(changeScope);
    if (containingFileByTree != null && !(containingFileByTree instanceof DummyHolder) && !manager.isCommitInProgress()) {
      PsiUtilCore.ensureValid(containingFileByTree);
    }

    boolean physical = changeScope.isPhysical();
    if (synchronizer.toProcessPsiEvent()) {
      // fail-fast to prevent any psi modifications that would cause psi/document text mismatch
      // PsiToDocumentSynchronizer assertions happen inside event processing and are logged by PsiManagerImpl.fireEvent instead of being rethrown
      // so it's important to throw something outside event processing
      if (isDocumentUncommitted(containingFileByTree)) {
        throw new IllegalStateException("Attempt to modify PSI for non-committed Document!");
      }
      CommandProcessor commandProcessor = CommandProcessor.getInstance();
      if (physical && !commandProcessor.isUndoTransparentActionInProgress() && commandProcessor.getCurrentCommand() == null) {
        throw new IncorrectOperationException(
          "Must not change PSI outside command or undo-transparent action. See consulo.ide.impl.idea.openapi.command.WriteCommandAction or consulo.ide.impl.idea.openapi.command.CommandProcessor");
      }
    }

    if (containingFileByTree != null) {
      ((SmartPointerManagerImpl)SmartPointerManager.getInstance(myProject)).fastenBelts(containingFileByTree.getViewProvider()
                                                                                                            .getVirtualFile());
      if (containingFileByTree instanceof PsiFileImpl) {
        ((PsiFileImpl)containingFileByTree).beforeAstChange();
      }
    }

    BlockSupportImpl.sendBeforeChildrenChangeEvent((PsiManagerImpl)PsiManager.getInstance(myProject), changeScope, true);
    Document document =
      containingFileByTree == null ? null : physical ? manager.getDocument(containingFileByTree) : manager.getCachedDocument(
        containingFileByTree);
    if (document != null) {
      synchronizer.startTransaction(myProject, document, changeScope);
    }
  }

  private boolean isDocumentUncommitted(@Nullable PsiFile file) {
    if (file == null) return false;

    PsiDocumentManager manager = PsiDocumentManager.getInstance(myProject);
    Document cachedDocument = manager.getCachedDocument(file);
    return cachedDocument != null && manager.isUncommited(cachedDocument);
  }

  @Nullable
  private static PsiFile getContainingFileByTree(@Nonnull final PsiElement changeScope) {
    // there could be pseudo physical trees (JSPX/JSP/etc.) which must not translate
    // any changes to document and not to fire any PSI events
    final PsiFile psiFile;
    final ASTNode node = changeScope.getNode();
    if (node == null) {
      psiFile = changeScope.getContainingFile();
    }
    else {
      final FileElement fileElement = TreeUtil.getFileElement((TreeElement)node);
      // assert fileElement != null : "Can't find file element for node: " + node;
      // Hack. the containing tree can be invalidated if updating supplementary trees like HTML in JSP.
      if (fileElement == null) return null;

      psiFile = (PsiFile)fileElement.getPsi();
    }
    return psiFile.getNode() != null ? psiFile : null;
  }
}
