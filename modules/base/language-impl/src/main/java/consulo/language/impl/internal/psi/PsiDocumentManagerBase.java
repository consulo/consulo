// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.util.Semaphore;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.DocumentRunnable;
import consulo.document.DocumentWindow;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.impl.DocumentImpl;
import consulo.document.impl.FrozenDocument;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.EditorDocumentPriorities;
import consulo.document.internal.PrioritizedDocumentListener;
import consulo.document.util.FileContentUtilCore;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.content.FileIndexFacade;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.file.FileManager;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.impl.internal.psi.diff.BlockSupport;
import consulo.language.impl.internal.psi.diff.BlockSupportImpl;
import consulo.language.impl.internal.psi.diff.DiffLog;
import consulo.language.impl.internal.psi.pointer.SmartPointerManagerImpl;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiDocumentListener;
import consulo.language.psi.internal.ExternalChangeAction;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.Maps;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public abstract class PsiDocumentManagerBase extends PsiDocumentManager implements DocumentListener, Disposable {
    static final Logger LOG = Logger.getInstance(PsiDocumentManagerBase.class);
    private static final Key<Document> HARD_REF_TO_DOCUMENT = Key.create("HARD_REFERENCE_TO_DOCUMENT");
    private static final Key<List<Runnable>> ACTION_AFTER_COMMIT = Key.create("ACTION_AFTER_COMMIT");

    protected final Project myProject;
    private final PsiManager myPsiManager;
    protected final DocumentCommitProcessor myDocumentCommitProcessor;
    protected final Set<Document> myUncommittedDocuments = ContainerUtil.newConcurrentSet();
    private final Map<Document, UncommittedInfo> myUncommittedInfos = ContainerUtil.newConcurrentMap();
    protected boolean myStopTrackingDocuments;
    private boolean myPerformBackgroundCommit = true;

    private volatile boolean myIsCommitInProgress;
    private static volatile boolean ourIsFullReparseInProgress;
    private final PsiToDocumentSynchronizer mySynchronizer;

    private final List<Listener> myListeners = Lists.newLockFreeCopyOnWriteList();

    protected PsiDocumentManagerBase(@Nonnull Project project, DocumentCommitProcessor documentCommitProcessor) {
        myProject = project;
        myPsiManager = PsiManager.getInstance(project);
        myDocumentCommitProcessor = documentCommitProcessor;
        mySynchronizer = new PsiToDocumentSynchronizer(this, project.getMessageBus());
        myPsiManager.addPsiTreeChangeListener(mySynchronizer);

        project.getMessageBus().connect(this)
            .subscribe(PsiDocumentTransactionListener.class, (document, file) -> myUncommittedDocuments.remove(document));
    }

    @Override
    public boolean isUnderSynchronization(@Nonnull Document document) {
        return getSynchronizer().isInSynchronization(document);
    }

    @Override
    @Nullable
    @RequiredReadAction
    public PsiFile getPsiFile(@Nonnull Document document) {
        if (document instanceof DocumentWindow documentWindow && !documentWindow.isValid()) {
            return null;
        }

        PsiFile psiFile = getCachedPsiFile(document);
        if (psiFile != null) {
            return ensureValidFile(psiFile, "Cached PSI");
        }

        final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }

        psiFile = getPsiFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        fireFileCreated(document, psiFile);

        return psiFile;
    }

    @Nonnull
    private static PsiFile ensureValidFile(@Nonnull PsiFile psiFile, @Nonnull String debugInfo) {
        if (!psiFile.isValid()) {
            throw new PsiInvalidElementAccessException(psiFile, debugInfo);
        }
        return psiFile;
    }

    public void associatePsi(@Nonnull Document document, @Nullable PsiFile file) {
        throw new UnsupportedOperationException();
    }

    @Override
    @RequiredReadAction
    public PsiFile getCachedPsiFile(@Nonnull Document document) {
        final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }
        return getCachedPsiFile(virtualFile);
    }

    @Nullable
    @RequiredReadAction
    public FileViewProvider getCachedViewProvider(@Nonnull Document document) {
        final VirtualFile virtualFile = getVirtualFile(document);
        if (virtualFile == null) {
            return null;
        }
        return getFileManager().findCachedViewProvider(virtualFile);
    }

    @Nullable
    private static VirtualFile getVirtualFile(@Nonnull Document document) {
        final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }
        return virtualFile;
    }

    @Nullable
    @RequiredReadAction
    public PsiFile getCachedPsiFile(@Nonnull VirtualFile virtualFile) {
        return getFileManager().getCachedPsiFile(virtualFile);
    }

    @Nullable
    @RequiredReadAction
    private PsiFile getPsiFile(@Nonnull VirtualFile virtualFile) {
        return getFileManager().findFile(virtualFile);
    }

    @Nonnull
    private FileManager getFileManager() {
        return ((PsiManagerEx) myPsiManager).getFileManager();
    }

    @Override
    @RequiredReadAction
    public Document getDocument(@Nonnull PsiFile file) {
        Document document = getCachedDocument(file);
        if (document != null) {
            if (!file.getViewProvider().isPhysical()) {
                PsiUtilCore.ensureValid(file);
                associatePsi(document, file);
            }
            return document;
        }

        FileViewProvider viewProvider = file.getViewProvider();
        if (!viewProvider.isEventSystemEnabled()) {
            return null;
        }

        document = FileDocumentManager.getInstance().getDocument(viewProvider.getVirtualFile());
        if (document != null) {
            if (document.getTextLength() != file.getTextLength()) {
                String message = "Document/PSI mismatch: " + file + " (" + file.getClass() + "); physical=" + viewProvider.isPhysical();
                if (document.getTextLength() + file.getTextLength() < 8096) {
                    message += "\n=== document ===\n" + document.getText() + "\n=== PSI ===\n" + file.getText();
                }
                throw new AssertionError(message);
            }

            if (!viewProvider.isPhysical()) {
                PsiUtilCore.ensureValid(file);
                associatePsi(document, file);
                file.putUserData(HARD_REF_TO_DOCUMENT, document);
            }
        }

        return document;
    }

    @Override
    public Document getCachedDocument(@Nonnull PsiFile file) {
        if (!file.isPhysical()) {
            return null;
        }
        VirtualFile vFile = file.getViewProvider().getVirtualFile();
        return FileDocumentManager.getInstance().getCachedDocument(vFile);
    }

    @Override
    public void commitAllDocuments() {
        Application.get().assertIsWriteThread();

        if (myUncommittedDocuments.isEmpty()) {
            return;
        }

        final Document[] documents = getUncommittedDocuments();
        for (Document document : documents) {
            if (isCommitted(document)) {
                boolean success = doCommitWithoutReparse(document);
                LOG.error("Committed document in uncommitted set: " + document + ", force-committed=" + success);
            }
            else if (!doCommit(document)) {
                LOG.error("Couldn't commit " + document);
            }
        }

        assertEverythingCommitted();
    }

    @Override
    public boolean commitAllDocumentsUnderProgress() {
        Application application = Application.get();
        //backward compatibility with unit tests
        if (application.isUnitTestMode()) {
            commitAllDocuments();
            return true;
        }
        assert !application.isWriteAccessAllowed() : "Do not call commitAllDocumentsUnderProgress inside write-action";
        final int semaphoreTimeoutInMs = 50;
        final Runnable commitAllDocumentsRunnable = () -> {
            Semaphore semaphore = new Semaphore(1);
            application.invokeLater(() -> PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(semaphore::up));
            while (!semaphore.waitFor(semaphoreTimeoutInMs)) {
                ProgressManager.checkCanceled();
            }
        };
        return ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(commitAllDocumentsRunnable, "Processing Documents", true, myProject);
    }

    private void assertEverythingCommitted() {
        LOG.assertTrue(!hasUncommitedDocuments(), myUncommittedDocuments);
    }

    @RequiredUIAccess
    public boolean doCommitWithoutReparse(@Nonnull Document document) {
        return finishCommitInWriteAction(document, Collections.emptyList(), Collections.emptyList(), true, true);
    }

    @Override
    public void performForCommittedDocument(@Nonnull final Document doc, @Nonnull final Runnable action) {
        Document document = getTopLevelDocument(doc);
        if (isCommitted(document)) {
            action.run();
        }
        else {
            addRunOnCommit(document, action);
        }
    }

    private final Map<Object, Runnable> actionsWhenAllDocumentsAreCommitted = new LinkedHashMap<>(); //accessed from EDT only
    private static final Object PERFORM_ALWAYS_KEY = ObjectUtil.sentinel("PERFORM_ALWAYS");

    /**
     * Cancel previously registered action and schedules (new) action to be executed when all documents are committed.
     *
     * @param key    the (unique) id of the action.
     * @param action The action to be executed after automatic commit.
     *               This action will overwrite any action which was registered under this key earlier.
     *               The action will be executed in EDT.
     * @return true if action has been run immediately, or false if action was scheduled for execution later.
     */
    @RequiredUIAccess
    public boolean cancelAndRunWhenAllCommitted(@Nonnull Object key, @Nonnull final Runnable action) {
        UIAccess.assertIsUIThread();
        if (myProject.isDisposed()) {
            action.run();
            return true;
        }
        if (myUncommittedDocuments.isEmpty()) {
            if (!isCommitInProgress()) {
                // in case of fireWriteActionFinished() we didn't execute 'actionsWhenAllDocumentsAreCommitted' yet
                assert actionsWhenAllDocumentsAreCommitted.isEmpty() : actionsWhenAllDocumentsAreCommitted;
            }
            action.run();
            return true;
        }

        checkWeAreOutsideAfterCommitHandler();

        actionsWhenAllDocumentsAreCommitted.put(key, action);
        return false;
    }

    public static void addRunOnCommit(@Nonnull Document document, @Nonnull Runnable action) {
        synchronized (ACTION_AFTER_COMMIT) {
            List<Runnable> list = document.getUserData(ACTION_AFTER_COMMIT);
            if (list == null) {
                document.putUserData(ACTION_AFTER_COMMIT, list = new SmartList<>());
            }
            list.add(action);
        }
    }

    private static List<Runnable> getAndClearActionsAfterCommit(@Nonnull Document document) {
        List<Runnable> list;
        synchronized (ACTION_AFTER_COMMIT) {
            list = document.getUserData(ACTION_AFTER_COMMIT);
            if (list != null) {
                list = new ArrayList<>(list);
                document.putUserData(ACTION_AFTER_COMMIT, null);
            }
        }
        return list;
    }

    @Override
    @RequiredUIAccess
    public void commitDocument(@Nonnull final Document doc) {
        final Document document = getTopLevelDocument(doc);

        if (isEventSystemEnabled(document)) {
            UIAccess.assertIsUIThread();
        }

        if (!isCommitted(document)) {
            doCommit(document);
        }
    }

    @RequiredReadAction
    public boolean isEventSystemEnabled(Document document) {
        FileViewProvider viewProvider = getCachedViewProvider(document);
        return viewProvider != null && viewProvider.isEventSystemEnabled() && !AbstractFileViewProvider.isFreeThreaded(viewProvider);
    }

    @RequiredUIAccess
    public boolean finishCommit(
        @Nonnull final Document document,
        @Nonnull List<? extends BooleanRunnable> finishProcessors,
        @Nonnull List<? extends BooleanRunnable> reparseInjectedProcessors,
        final boolean synchronously,
        @Nonnull final Object reason
    ) {
        assert !myProject.isDisposed() : "Already disposed";
        UIAccess.assertIsUIThread();
        final boolean[] ok = {true};
        Runnable runnable = new DocumentRunnable(document, myProject) {
            @Override
            public void run() {
                ok[0] = finishCommitInWriteAction(document, finishProcessors, reparseInjectedProcessors, synchronously, false);
            }
        };
        if (synchronously) {
            runnable.run();
        }
        else {
            Application.get().runWriteAction(runnable);
        }

        if (ok[0]) {
            // run after commit actions outside write action
            runAfterCommitActions(document);
            if (DebugUtil.DO_EXPENSIVE_CHECKS) {
                checkAllElementsValid(document, reason);
            }
        }
        return ok[0];
    }

    @RequiredUIAccess
    protected boolean finishCommitInWriteAction(
        @Nonnull final Document document,
        @Nonnull List<? extends BooleanRunnable> finishProcessors,
        @Nonnull List<? extends BooleanRunnable> reparseInjectedProcessors,
        final boolean synchronously,
        boolean forceNoPsiCommit
    ) {
        UIAccess.assertIsUIThread();
        if (myProject.isDisposed()) {
            return false;
        }
        assert !(document instanceof DocumentWindow);

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile != null) {
            getSmartPointerManager().fastenBelts(virtualFile);
        }

        FileViewProvider viewProvider = forceNoPsiCommit ? null : getCachedViewProvider(document);

        myIsCommitInProgress = true;
        SimpleReference<Boolean> success = new SimpleReference<>(true);
        try {
            ProgressManager.getInstance().executeNonCancelableSection(() -> {
                if (viewProvider == null) {
                    handleCommitWithoutPsi(document);
                }
                else {
                    success.set(commitToExistingPsi(document, finishProcessors, reparseInjectedProcessors, synchronously, virtualFile));
                }
            });
        }
        catch (Throwable e) {
            try {
                forceReload(virtualFile, viewProvider);
            }
            finally {
                LOG.error(e);
            }
        }
        finally {
            if (success.get()) {
                myUncommittedDocuments.remove(document);
            }
            myIsCommitInProgress = false;
        }

        return success.get();
    }

    @RequiredReadAction
    private boolean commitToExistingPsi(
        @Nonnull Document document,
        @Nonnull List<? extends BooleanRunnable> finishProcessors,
        @Nonnull List<? extends BooleanRunnable> reparseInjectedProcessors,
        boolean synchronously,
        @Nullable VirtualFile virtualFile
    ) {
        for (BooleanRunnable finishRunnable : finishProcessors) {
            boolean success = finishRunnable.run();
            if (synchronously) {
                assert success : finishRunnable + " in " + finishProcessors;
            }
            if (!success) {
                return false;
            }
        }
        clearUncommittedInfo(document);
        if (virtualFile != null) {
            getSmartPointerManager().updatePointerTargetsAfterReparse(virtualFile);
        }
        FileViewProvider viewProvider = getCachedViewProvider(document);
        if (viewProvider != null) {
            viewProvider.contentsSynchronized();
        }
        for (BooleanRunnable runnable : reparseInjectedProcessors) {
            if (!runnable.run()) {
                return false;
            }
        }
        return true;
    }

    public void forceReload(VirtualFile virtualFile, @Nullable FileViewProvider viewProvider) {
        if (viewProvider != null) {
            ((AbstractFileViewProvider) viewProvider).markInvalidated();
        }
        if (virtualFile != null) {
            getFileManager().forceReload(virtualFile);
        }
    }

    private void checkAllElementsValid(@Nonnull Document document, @Nonnull final Object reason) {
        final PsiFile psiFile = getCachedPsiFile(document);
        if (psiFile != null) {
            psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (!element.isValid()) {
                        throw new AssertionError(
                            "Commit to '" + psiFile.getVirtualFile() + "' has led to invalid element: " + element +
                                "; Reason: '" + reason + "'"
                        );
                    }
                }
            });
        }
    }

    @RequiredUIAccess
    private boolean doCommit(@Nonnull final Document document) {
        assert !myIsCommitInProgress : "Do not call commitDocument() from inside PSI change listener";

        // otherwise there are many clients calling commitAllDocs() on PSI childrenChanged()
        if (getSynchronizer().isDocumentAffectedByTransactions(document)) {
            return false;
        }

        final PsiFile psiFile = getPsiFile(document);
        if (psiFile == null) {
            myUncommittedDocuments.remove(document);
            runAfterCommitActions(document);
            return true; // the project must be closing or file deleted
        }

        Application.get().runWriteAction(() -> {
            myIsCommitInProgress = true;
            try {
                myDocumentCommitProcessor.commitSynchronously(document, myProject, psiFile);
            }
            finally {
                myIsCommitInProgress = false;
            }
            assert !isInUncommittedSet(document) : "Document :" + document;
        });
        return true;
    }

    // true if the PSI is being modified and events being sent
    public boolean isCommitInProgress() {
        return myIsCommitInProgress || isFullReparseInProgress();
    }

    public static boolean isFullReparseInProgress() {
        return ourIsFullReparseInProgress;
    }

    @Override
    public <T> T commitAndRunReadAction(@Nonnull final Supplier<T> computation) {
        final SimpleReference<T> ref = SimpleReference.create(null);
        commitAndRunReadAction(() -> ref.set(computation.get()));
        return ref.get();
    }

    @Override
    @RequiredUIAccess
    public void reparseFiles(@Nonnull Collection<? extends VirtualFile> files, boolean includeOpenFiles) {
        FileContentUtilCore.reparseFiles(files);
    }

    @Override
    public void commitAndRunReadAction(@Nonnull final Runnable runnable) {
        final Application application = Application.get();
        if (application.isDispatchThread()) {
            commitAllDocuments();
            runnable.run();
            return;
        }

        if (application.isReadAccessAllowed()) {
            LOG.error("Don't call commitAndRunReadAction inside ReadAction, it will cause a deadlock. " + Thread.currentThread());
        }

        while (true) {
            boolean executed = ReadAction.compute(() -> {
                if (myUncommittedDocuments.isEmpty()) {
                    runnable.run();
                    return true;
                }
                return false;
            });
            if (executed) {
                break;
            }

            ModalityState modality = myProject.getApplication().getDefaultModalityState();
            Semaphore semaphore = new Semaphore(1);
            AppUIExecutor.onWriteThread(ModalityState.any()).submit(() -> {
                if (myProject.isDisposed()) {
                    // committedness doesn't matter anymore; give clients a chance to do checkCanceled
                    semaphore.up();
                    return;
                }

                performWhenAllCommitted(modality, () -> semaphore.up());
            });

            while (!semaphore.waitFor(10)) {
                ProgressManager.checkCanceled();
            }
        }
    }

    /**
     * Schedules action to be executed when all documents are committed.
     *
     * @return true if action has been run immediately, or false if action was scheduled for execution later.
     */
    @Override
    @RequiredUIAccess
    public boolean performWhenAllCommitted(@Nonnull final Runnable action) {
        return performWhenAllCommitted(myProject.getApplication().getDefaultModalityState(), action);
    }

    @RequiredUIAccess
    private boolean performWhenAllCommitted(@Nonnull ModalityState modality, @Nonnull Runnable action) {
        UIAccess.assertIsUIThread();
        checkWeAreOutsideAfterCommitHandler();

        assert !myProject.isDisposed() : "Already disposed: " + myProject;
        if (myUncommittedDocuments.isEmpty()) {
            action.run();
            return true;
        }
        CompositeRunnable actions = (CompositeRunnable) actionsWhenAllDocumentsAreCommitted.get(PERFORM_ALWAYS_KEY);
        if (actions == null) {
            actions = new CompositeRunnable();
            actionsWhenAllDocumentsAreCommitted.put(PERFORM_ALWAYS_KEY, actions);
        }
        actions.add(action);

        if (modality != ModalityState.nonModal()) {
            // this client obviously expects all documents to be committed ASAP even inside modal dialog
            for (Document document : myUncommittedDocuments) {
                retainProviderAndCommitAsync(document, "re-added because performWhenAllCommitted(" + modality + ") was called", modality);
            }
        }
        return false;
    }

    @Override
    public void performLaterWhenAllCommitted(@Nonnull final Runnable runnable) {
        performLaterWhenAllCommitted(runnable, Application.get().getDefaultModalityState());
    }

    @Override
    public void performLaterWhenAllCommitted(@Nonnull final Runnable runnable, final ModalityState modalityState) {
        final Runnable whenAllCommitted = () -> Application.get().invokeLater(
            () -> {
                if (hasUncommitedDocuments()) {
                    // no luck, will try later
                    performLaterWhenAllCommitted(runnable);
                }
                else {
                    runnable.run();
                }
            },
            modalityState,
            myProject.getDisposed()
        );
        if (Application.get().isDispatchThread() && isInsideCommitHandler()) {
            whenAllCommitted.run();
        }
        else {
            Application.get().getLastUIAccess().giveIfNeed(() -> {
                if (!myProject.isDisposed()) {
                    performWhenAllCommitted(whenAllCommitted);
                }
            });
        }
    }

    private static class CompositeRunnable extends ArrayList<Runnable> implements Runnable {
        @Override
        public void run() {
            for (Runnable runnable : this) {
                runnable.run();
            }
        }
    }

    @RequiredUIAccess
    private void runAfterCommitActions(@Nonnull Document document) {
        if (!Application.get().isDispatchThread()) {
            // have to run in EDT to guarantee data structure safe access and "execute in EDT" callbacks contract
            Application.get().invokeLater(() -> {
                if (!myProject.isDisposed() && isCommitted(document)) {
                    runAfterCommitActions(document);
                }
            });
            return;
        }
        UIAccess.assertIsUIThread();
        List<Runnable> list = getAndClearActionsAfterCommit(document);
        if (list != null) {
            for (final Runnable runnable : list) {
                runnable.run();
            }
        }

        if (!hasUncommitedDocuments() && !actionsWhenAllDocumentsAreCommitted.isEmpty()) {
            List<Runnable> actions = new ArrayList<>(actionsWhenAllDocumentsAreCommitted.values());
            beforeCommitHandler();
            List<Pair<Runnable, Throwable>> exceptions = new ArrayList<>();
            try {
                for (Runnable action : actions) {
                    try {
                        action.run();
                    }
                    catch (ProcessCanceledException e) {
                        // some actions are crazy enough to use PCE for their own control flow.
                        // swallow and ignore to not disrupt completely unrelated control flow.
                    }
                    catch (Throwable e) {
                        exceptions.add(Pair.create(action, e));
                    }
                }
            }
            finally {
                // unblock adding listeners
                actionsWhenAllDocumentsAreCommitted.clear();
            }
            for (Pair<Runnable, Throwable> pair : exceptions) {
                Runnable action = pair.getFirst();
                Throwable e = pair.getSecond();
                LOG.error("During running " + action, e);
            }
        }
    }

    private void beforeCommitHandler() {
        actionsWhenAllDocumentsAreCommitted.put(
            PERFORM_ALWAYS_KEY,
            EmptyRunnable.getInstance()
        ); // to prevent listeners from registering new actions during firing
    }

    private void checkWeAreOutsideAfterCommitHandler() {
        if (isInsideCommitHandler()) {
            throw new IncorrectOperationException(
                "You must not call performWhenAllCommitted()/cancelAndRunWhenCommitted() from within after-commit handler");
        }
    }

    private boolean isInsideCommitHandler() {
        return actionsWhenAllDocumentsAreCommitted.get(PERFORM_ALWAYS_KEY) == EmptyRunnable.getInstance();
    }

    @Override
    public void addListener(@Nonnull Listener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull Listener listener) {
        myListeners.remove(listener);
    }

    @Override
    public boolean isDocumentBlockedByPsi(@Nonnull Document doc) {
        return false;
    }

    @Override
    public void doPostponedOperationsAndUnblockDocument(@Nonnull Document doc) {
    }

    protected void fireDocumentCreated(@Nonnull Document document, PsiFile file) {
        myProject.getMessageBus().syncPublisher(PsiDocumentListener.class).documentCreated(document, file, myProject);
        for (Listener listener : myListeners) {
            listener.documentCreated(document, file);
        }
    }

    private void fireFileCreated(@Nonnull Document document, @Nonnull PsiFile file) {
        myProject.getMessageBus().syncPublisher(PsiDocumentListener.class).fileCreated(file, document);
        for (Listener listener : myListeners) {
            listener.fileCreated(file, document);
        }
    }

    @Override
    @Nonnull
    public CharSequence getLastCommittedText(@Nonnull Document document) {
        return getLastCommittedDocument(document).getImmutableCharSequence();
    }

    @Override
    public long getLastCommittedStamp(@Nonnull Document document) {
        return getLastCommittedDocument(getTopLevelDocument(document)).getModificationStamp();
    }

    @Override
    @Nullable
    @RequiredReadAction
    public Document getLastCommittedDocument(@Nonnull PsiFile file) {
        Document document = getDocument(file);
        return document == null ? null : getLastCommittedDocument(document);
    }

    @Nonnull
    public DocumentEx getLastCommittedDocument(@Nonnull Document document) {
        if (document instanceof FrozenDocument) {
            return (DocumentEx) document;
        }

        if (document instanceof DocumentWindow) {
            DocumentWindow window = (DocumentWindow) document;
            Document delegate = window.getDelegate();
            if (delegate instanceof FrozenDocument) {
                return (DocumentEx) window;
            }

            if (!window.isValid()) {
                throw new AssertionError("host committed: " + isCommitted(delegate) + ", window=" + window);
            }

            UncommittedInfo info = myUncommittedInfos.get(delegate);
            DocumentWindow answer = info == null ? null : info.myFrozenWindows.get(document);
            if (answer == null) {
                answer = freezeWindow(window);
            }
            if (info != null) {
                answer = Maps.cacheOrGet(info.myFrozenWindows, window, answer);
            }
            return (DocumentEx) answer;
        }

        assert document instanceof DocumentImpl;
        UncommittedInfo info = myUncommittedInfos.get(document);
        return info != null ? info.myFrozen : ((DocumentImpl) document).freeze();
    }

    @Nonnull
    protected DocumentWindow freezeWindow(@Nonnull DocumentWindow document) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public List<DocumentEvent> getEventsSinceCommit(@Nonnull Document document) {
        assert document instanceof DocumentImpl : document;
        UncommittedInfo info = myUncommittedInfos.get(document);
        if (info != null) {
            //noinspection unchecked
            return (List<DocumentEvent>) info.myEvents.clone();
        }
        return Collections.emptyList();

    }

    @Override
    @Nonnull
    @RequiredReadAction
    public Document[] getUncommittedDocuments() {
        Application.get().assertReadAccessAllowed();
        //noinspection UnnecessaryLocalVariable
        Document[] documents = myUncommittedDocuments.toArray(Document.EMPTY_ARRAY);
        return documents; // java.util.ConcurrentHashMap.keySet().toArray() guaranteed to return array with no nulls
    }

    public boolean isInUncommittedSet(@Nonnull Document document) {
        return myUncommittedDocuments.contains(getTopLevelDocument(document));
    }

    @Override
    public boolean isUncommited(@Nonnull Document document) {
        return !isCommitted(document);
    }

    @Override
    public boolean isCommitted(@Nonnull Document document) {
        document = getTopLevelDocument(document);
        if (getSynchronizer().isInSynchronization(document)) {
            return true;
        }
        return !(document instanceof DocumentEx documentEx && documentEx.isInEventsHandling()) && !isInUncommittedSet(document);
    }

    @Nonnull
    private static Document getTopLevelDocument(@Nonnull Document document) {
        return document instanceof DocumentWindow ? ((DocumentWindow) document).getDelegate() : document;
    }

    @Override
    public boolean hasUncommitedDocuments() {
        return !myIsCommitInProgress && !myUncommittedDocuments.isEmpty();
    }

    @Override
    @RequiredReadAction
    public void beforeDocumentChange(@Nonnull DocumentEvent event) {
        if (myStopTrackingDocuments || myProject.isDisposed()) {
            return;
        }

        final Document document = event.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        boolean isRelevant = virtualFile != null && isRelevant(virtualFile);

        if (document instanceof DocumentImpl && !myUncommittedInfos.containsKey(document)) {
            myUncommittedInfos.put(document, new UncommittedInfo((DocumentImpl) document));
        }

        final FileViewProvider viewProvider = getCachedViewProvider(document);
        boolean inMyProject = viewProvider != null && viewProvider.getManager() == myPsiManager;
        if (!isRelevant || !inMyProject) {
            return;
        }

        final List<PsiFile> files = viewProvider.getAllFiles();
        PsiFile psiCause = null;
        for (PsiFile file : files) {
            if (file == null) {
                throw new AssertionError(
                    "View provider " + viewProvider + " (" + viewProvider.getClass() + ") returned null in its files array: " +
                        files + " for file " + viewProvider.getVirtualFile()
                );
            }

            if (PsiToDocumentSynchronizer.isInsideAtomicChange(file)) {
                psiCause = file;
            }
        }

        if (psiCause == null) {
            beforeDocumentChangeOnUnlockedDocument(viewProvider);
        }
    }

    protected void beforeDocumentChangeOnUnlockedDocument(@Nonnull final FileViewProvider viewProvider) {
    }

    @Override
    @RequiredUIAccess
    public void documentChanged(@Nonnull DocumentEvent event) {
        if (myStopTrackingDocuments || myProject.isDisposed()) {
            return;
        }

        final Document document = event.getDocument();

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        boolean isRelevant = virtualFile != null && isRelevant(virtualFile);

        final FileViewProvider viewProvider = getCachedViewProvider(document);
        if (viewProvider == null) {
            handleCommitWithoutPsi(document);
            return;
        }
        boolean inMyProject = viewProvider.getManager() == myPsiManager;
        if (!isRelevant || !inMyProject) {
            clearUncommittedInfo(document);
            return;
        }

        List<PsiFile> files = viewProvider.getAllFiles();
        if (files.isEmpty()) {
            handleCommitWithoutPsi(document);
            return;
        }

        boolean commitNecessary =
            files.stream().noneMatch(file -> PsiToDocumentSynchronizer.isInsideAtomicChange(file) || !(file instanceof PsiFileImpl));

        boolean forceCommit = Application.get().hasWriteAction(ExternalChangeAction.class)
            && (SystemProperties.getBooleanProperty("idea.force.commit.on.external.change", false)
            || Application.get().isHeadlessEnvironment() && !Application.get().isUnitTestMode());

        // Consider that it's worth to perform complete re-parse instead of merge if the whole document text is replaced and
        // current document lines number is roughly above 5000. This makes sense in situations when external change is performed
        // for the huge file (that causes the whole document to be reloaded and 'merge' way takes a while to complete).
        if (event.isWholeTextReplaced() && document.getTextLength() > 100000) {
            document.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
        }

        if (commitNecessary) {
            assert !(document instanceof DocumentWindow);
            myUncommittedDocuments.add(document);
            if (forceCommit) {
                commitDocument(document);
            }
            else if (!document.isInBulkUpdate() && myPerformBackgroundCommit) {
                myDocumentCommitProcessor.commitAsynchronously(
                    myProject,
                    document,
                    event,
                    myProject.getApplication().getDefaultModalityState()
                );
            }
        }
        else {
            clearUncommittedInfo(document);
        }
    }

    @Override
    public void bulkUpdateStarting(@Nonnull Document document) {
        document.putUserData(BlockSupport.DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
    }

    @Override
    public void bulkUpdateFinished(@Nonnull Document document) {
        retainProviderAndCommitAsync(document, "Bulk update finished", myProject.getApplication().getDefaultModalityState());
    }

    private void retainProviderAndCommitAsync(
        @Nonnull Document document,
        @Nonnull Object reason,
        @Nonnull ModalityState modality
    ) {
        myDocumentCommitProcessor.commitAsynchronously(
            myProject,
            document,
            reason,
            modality
        );
    }

    public class PriorityEventCollector implements PrioritizedDocumentListener {
        @Override
        public int getPriority() {
            return EditorDocumentPriorities.RANGE_MARKER;
        }

        @Override
        public void documentChanged(@Nonnull DocumentEvent event) {
            UncommittedInfo info = myUncommittedInfos.get(event.getDocument());
            if (info != null) {
                info.myEvents.add(event);
            }
        }
    }

    @RequiredUIAccess
    public void handleCommitWithoutPsi(@Nonnull Document document) {
        final UncommittedInfo prevInfo = clearUncommittedInfo(document);
        if (prevInfo == null) {
            return;
        }

        myUncommittedDocuments.remove(document);

        if (!myProject.isInitialized() || myProject.isDisposed() || myProject.isDefault()) {
            return;
        }

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile != null) {
            FileManager fileManager = getFileManager();
            FileViewProvider viewProvider = fileManager.findCachedViewProvider(virtualFile);
            if (viewProvider != null) {
                // we can end up outside write action here if the document has forUseInNonAWTThread=true
                Application.get().runWriteAction(((AbstractFileViewProvider) viewProvider)::onContentReload);
            }
            else if (FileIndexFacade.getInstance(myProject).isInContent(virtualFile)) {
                Application.get().runWriteAction((Runnable) ((FileManagerImpl) fileManager)::firePropertyChangedForUnloadedPsi);
            }
        }

        runAfterCommitActions(document);
    }

    @Nullable
    private UncommittedInfo clearUncommittedInfo(@Nonnull Document document) {
        UncommittedInfo info = myUncommittedInfos.remove(document);
        if (info != null) {
            getSmartPointerManager().updatePointers(document, info.myFrozen, info.myEvents);
        }
        return info;
    }

    private SmartPointerManagerImpl getSmartPointerManager() {
        return (SmartPointerManagerImpl) SmartPointerManager.getInstance(myProject);
    }

    private boolean isRelevant(@Nonnull VirtualFile virtualFile) {
        return !myProject.isDisposed() && !virtualFile.getFileType().isBinary();
    }

    @Override
    @RequiredReadAction
    public boolean checkConsistency(@Nonnull PsiFile psiFile, @Nonnull Document document) {
        //todo hack
        if (psiFile.getVirtualFile() == null) {
            return true;
        }

        CharSequence editorText = document.getCharsSequence();
        int documentLength = document.getTextLength();
        if (psiFile.textMatches(editorText)) {
            LOG.assertTrue(psiFile.getTextLength() == documentLength);
            return true;
        }

        char[] fileText = psiFile.textToCharArray();
        @SuppressWarnings("NonConstantStringShouldBeStringBuffer") String error =
            "File '" + psiFile.getName() + "' text mismatch after reparse. " +
                "File length=" + fileText.length + "; Doc length=" + documentLength + "\n";
        int i = 0;
        for (; i < documentLength; i++) {
            if (i >= fileText.length) {
                error += "editorText.length > psiText.length i=" + i + "\n";
                break;
            }
            if (i >= editorText.length()) {
                error += "editorText.length > psiText.length i=" + i + "\n";
                break;
            }
            if (editorText.charAt(i) != fileText[i]) {
                error += "first unequal char i=" + i + "\n";
                break;
            }
        }
        //error += "*********************************************" + "\n";
        //if (i <= 500){
        //  error += "Equal part:" + editorText.subSequence(0, i) + "\n";
        //}
        //else{
        //  error += "Equal part start:\n" + editorText.subSequence(0, 200) + "\n";
        //  error += "................................................" + "\n";
        //  error += "................................................" + "\n";
        //  error += "................................................" + "\n";
        //  error += "Equal part end:\n" + editorText.subSequence(i - 200, i) + "\n";
        //}
        error += "*********************************************" + "\n";
        error += "Editor Text tail:(" + (documentLength - i) + ")\n";
        // + editorText.subSequence(i, Math.min(i + 300, documentLength)) + "\n";
        error += "*********************************************" + "\n";
        error += "Psi Text tail:(" + (fileText.length - i) + ")\n";
        error += "*********************************************" + "\n";

        if (document instanceof DocumentWindow) {
            error += "doc: '" + document.getText() + "'\n";
            error += "psi: '" + psiFile.getText() + "'\n";
            error += "ast: '" + psiFile.getNode().getText() + "'\n";
            error += psiFile.getLanguage() + "\n";
            PsiElement context = InjectedLanguageManager.getInstance(psiFile.getProject()).getInjectionHost(psiFile);
            if (context != null) {
                error += "context: " + context + "; text: '" + context.getText() + "'\n";
                error += "context file: " + context.getContainingFile() + "\n";
            }
            error += "document window ranges: " + Arrays.asList(((DocumentWindow) document).getHostRanges()) + "\n";
        }
        LOG.error(error);
        //document.replaceString(0, documentLength, psiFile.getText());
        return false;
    }

    @TestOnly
    public void clearUncommittedDocuments() {
        myUncommittedInfos.clear();
        myUncommittedDocuments.clear();
        mySynchronizer.cleanupForNextTest();
    }

    @TestOnly
    public void disableBackgroundCommit(@Nonnull Disposable parentDisposable) {
        assert myPerformBackgroundCommit;
        myPerformBackgroundCommit = false;
        Disposer.register(parentDisposable, () -> myPerformBackgroundCommit = true);
    }

    @Override
    public void dispose() {
        clearUncommittedDocuments();
    }

    @Nonnull
    public PsiToDocumentSynchronizer getSynchronizer() {
        return mySynchronizer;
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @RequiredUIAccess
    public void reparseFileFromText(@Nonnull PsiFileImpl file) {
        UIAccess.assertIsUIThread();
        if (isCommitInProgress()) {
            throw new IllegalStateException("Re-entrant commit is not allowed");
        }

        FileElement node = file.calcTreeElement();
        CharSequence text = node.getChars();
        ourIsFullReparseInProgress = true;
        try {
            WriteAction.run(() -> {
                ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
                if (indicator == null) {
                    indicator = new EmptyProgressIndicator();
                }
                DiffLog log = BlockSupportImpl.makeFullParse(file, node, text, indicator, text).log;
                log.doActualPsiChange(file);
                file.getViewProvider().contentsSynchronized();
            });
        }
        finally {
            ourIsFullReparseInProgress = false;
        }
    }

    private static class UncommittedInfo {
        private final FrozenDocument myFrozen;
        private final ArrayList<DocumentEvent> myEvents = new ArrayList<>();
        private final ConcurrentMap<DocumentWindow, DocumentWindow> myFrozenWindows = ContainerUtil.newConcurrentMap();

        private UncommittedInfo(@Nonnull DocumentImpl original) {
            myFrozen = original.freeze();
        }
    }

    @Nonnull
    public List<BooleanRunnable> reparseChangedInjectedFragments(
        @Nonnull Document hostDocument,
        @Nonnull PsiFile hostPsiFile,
        @Nonnull TextRange range,
        @Nonnull ProgressIndicator indicator,
        @Nonnull ASTNode oldRoot,
        @Nonnull ASTNode newRoot
    ) {
        return Collections.emptyList();
    }

    @TestOnly
    public boolean isDefaultProject() {
        return myProject.isDefault();
    }
}
