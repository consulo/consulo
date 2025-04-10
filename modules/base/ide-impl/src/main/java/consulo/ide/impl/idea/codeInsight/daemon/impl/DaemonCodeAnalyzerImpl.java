// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.PowerSaveMode;
import consulo.application.impl.internal.ModalityStateImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.component.ProcessCanceledException;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.highlight.HighlightingPass;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.idea.codeInsight.daemon.DaemonCodeAnalyzerSettingsImpl;
import consulo.ide.impl.idea.codeInsight.intention.impl.FileLevelIntentionComponent;
import consulo.ide.impl.idea.codeInsight.intention.impl.IntentionHintComponent;
import consulo.ide.impl.idea.openapi.application.impl.ApplicationInfoImpl;
import consulo.ide.impl.idea.openapi.application.impl.NonBlockingReadActionImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.AsyncEditorLoader;
import consulo.ide.impl.idea.openapi.fileTypes.impl.FileTypeManagerImpl;
import consulo.language.editor.*;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassManager;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.impl.internal.daemon.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.editor.impl.internal.highlight.GeneralHighlightingPass;
import consulo.language.editor.impl.internal.highlight.HighlightingSessionImpl;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.file.FileTypeManager;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.language.psi.resolve.RefResolveService;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class also controls the auto-reparse and auto-hints.
 */
@Singleton
@State(name = "DaemonCodeAnalyzer", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class DaemonCodeAnalyzerImpl extends DaemonCodeAnalyzerEx implements PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(DaemonCodeAnalyzerImpl.class);

    private static final Key<List<HighlightInfoImpl>> FILE_LEVEL_HIGHLIGHTS = Key.create("FILE_LEVEL_HIGHLIGHTS");
    private final Project myProject;
    private final DaemonCodeAnalyzerSettings mySettings;
    @Nonnull
    private final PsiDocumentManager myPsiDocumentManager;
    private DaemonProgressIndicator myUpdateProgress = new DaemonProgressIndicator(); //guarded by this

    private final UpdateRunnable myUpdateRunnable;

    @Nonnull
    private volatile Future<?> myUpdateRunnableFuture = CompletableFuture.completedFuture(null);
    private boolean myUpdateByTimerEnabled = true; // guarded by this
    private final Collection<VirtualFile> myDisabledHintsFiles = new HashSet<>();
    private final Collection<VirtualFile> myDisabledHighlightingFiles = new HashSet<>();

    private final FileStatusMapImpl myFileStatusMap;
    private DaemonCodeAnalyzerSettings myLastSettings;

    private volatile boolean myDisposed;     // the only possible transition: false -> true
    private volatile boolean myInitialized;  // the only possible transition: false -> true

    private static final String DISABLE_HINTS_TAG = "disable_hints";
    private static final String FILE_TAG = "file";
    private static final String URL_ATT = "url";
    private final PassExecutorService myPassExecutorService;

    @Inject
    public DaemonCodeAnalyzerImpl(@Nonnull Project project) {
        // DependencyValidationManagerImpl adds scope listener, so, we need to force service creation
        DependencyValidationManager.getInstance(project);

        myProject = project;
        mySettings = DaemonCodeAnalyzerSettings.getInstance();
        myPsiDocumentManager = PsiDocumentManager.getInstance(myProject);
        myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)mySettings).clone();

        myFileStatusMap = new FileStatusMapImpl(project);
        myPassExecutorService = new PassExecutorService(project);
        Disposer.register(this, myPassExecutorService);
        Disposer.register(this, myFileStatusMap);
        //noinspection TestOnlyProblems
        DaemonProgressIndicator.setDebug(LOG.isDebugEnabled());

        assert !myInitialized : "Double Initializing";
        Disposer.register(this, new StatusBarUpdater(project));

        myInitialized = true;
        myDisposed = false;
        myFileStatusMap.markAllFilesDirty("DCAI init");
        myUpdateRunnable = new UpdateRunnable(myProject);
        Disposer.register(this, () -> {
            assert myInitialized : "Disposing not initialized component";
            assert !myDisposed : "Double dispose";
            myUpdateRunnable.clearFieldsOnDispose();

            stopProcess(false, "Dispose");

            myDisposed = true;
            myLastSettings = null;
        });
    }

    @Override
    public synchronized void dispose() {
        clearReferences();
    }

    private synchronized void clearReferences() {
        myUpdateProgress = new DaemonProgressIndicator(); // leak of highlight session via user data
        myUpdateRunnableFuture.cancel(true);
    }

    @Nonnull
    @TestOnly
    @RequiredReadAction
    public static List<HighlightInfo> getHighlights(
        @Nonnull Document document,
        @Nullable HighlightSeverity minSeverity,
        @Nonnull Project project
    ) {
        List<HighlightInfo> infos = new ArrayList<>();
        processHighlights(document, project, minSeverity, 0, document.getTextLength(), Processors.cancelableCollectProcessor(infos));
        return infos;
    }

    @Override
    @Nonnull
    @TestOnly
    public List<HighlightInfo> getFileLevelHighlights(@Nonnull Project project, @Nonnull PsiFile file) {
        VirtualFile vFile = file.getViewProvider().getVirtualFile();
        return Arrays.stream(FileEditorManager.getInstance(project).getEditors(vFile))
            .map(fileEditor -> fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Override
    public void cleanFileLevelHighlights(@Nonnull Project project, int group, PsiFile psiFile) {
        if (psiFile == null) {
            return;
        }
        FileViewProvider provider = psiFile.getViewProvider();
        VirtualFile vFile = provider.getVirtualFile();
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor fileEditor : manager.getEditors(vFile)) {
            List<HighlightInfoImpl> infos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
            if (infos == null) {
                continue;
            }
            List<HighlightInfoImpl> infosToRemove = new ArrayList<>();
            for (HighlightInfoImpl info : infos) {
                if (info.getGroup() == group) {
                    manager.removeTopComponent(fileEditor, info.fileLevelComponent);
                    infosToRemove.add(info);
                }
            }
            infos.removeAll(infosToRemove);
        }
    }

    @Override
    @RequiredUIAccess
    public void addFileLevelHighlight(
        @Nonnull Project project,
        int group,
        @Nonnull HighlightInfo i,
        @Nonnull PsiFile psiFile
    ) {
        HighlightInfoImpl info = (HighlightInfoImpl)i;

        VirtualFile vFile = psiFile.getViewProvider().getVirtualFile();
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor fileEditor : manager.getEditors(vFile)) {
            if (fileEditor instanceof TextEditor textEditor) {
                FileLevelIntentionComponent component = new FileLevelIntentionComponent(
                    info.getDescription(),
                    info.getSeverity(),
                    info.getGutterIconRenderer(),
                    info.quickFixActionRanges,
                    project,
                    psiFile,
                    textEditor.getEditor(),
                    info.getToolTip()
                );
                manager.addTopComponent(fileEditor, component);
                List<HighlightInfoImpl> fileLevelInfos = fileEditor.getUserData(FILE_LEVEL_HIGHLIGHTS);
                if (fileLevelInfos == null) {
                    fileLevelInfos = new ArrayList<>();
                    fileEditor.putUserData(FILE_LEVEL_HIGHLIGHTS, fileLevelInfos);
                }
                info.fileLevelComponent = component;
                info.setGroup(group);
                fileLevelInfos.add(info);
            }
        }
    }

    @Override
    @Nonnull
    public List<HighlightInfo> runMainPasses(
        @Nonnull PsiFile psiFile,
        @Nonnull Document document,
        @Nonnull ProgressIndicator progress
    ) {
        Application app = myProject.getApplication();
        if (app.isDispatchThread()) {
            throw new IllegalStateException("Must not run highlighting from under EDT");
        }
        if (!app.isReadAccessAllowed()) {
            throw new IllegalStateException("Must run highlighting from under read action");
        }
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (!(indicator instanceof DaemonProgressIndicator)) {
            throw new IllegalStateException("Must run highlighting under progress with DaemonProgressIndicator");
        }
        // clear status maps to run passes from scratch so that refCountHolder won't conflict and try to restart itself on partially filled maps
        myFileStatusMap.markAllFilesDirty("prepare to run main passes");
        stopProcess(false, "disable background daemon");
        myPassExecutorService.cancelAll(true);

        List<HighlightInfo> result;
        try {
            result = new ArrayList<>();
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null && !virtualFile.getFileType().isBinary()) {
                List<TextEditorHighlightingPass> passes = TextEditorHighlightingPassManager.getInstance(myProject)
                    .instantiateMainPasses(psiFile, document, HighlightInfoProcessor.getEmpty());

                passes.sort((o1, o2) -> {
                    if (o1 instanceof GeneralHighlightingPass) {
                        return -1;
                    }
                    if (o2 instanceof GeneralHighlightingPass) {
                        return 1;
                    }
                    return 0;
                });

                try {
                    for (TextEditorHighlightingPass pass : passes) {
                        pass.doCollectInformation(progress);
                        result.addAll(pass.getInfos());
                    }
                }
                catch (ProcessCanceledException e) {
                    LOG.debug("Canceled: " + progress);
                    throw e;
                }
            }
        }
        finally {
            stopProcess(true, "re-enable background daemon after main passes run");
        }

        return result;
    }

    private volatile boolean mustWaitForSmartMode = true;

    @TestOnly
    public void mustWaitForSmartMode(boolean mustWait, @Nonnull Disposable parent) {
        boolean old = mustWaitForSmartMode;
        mustWaitForSmartMode = mustWait;
        Disposer.register(parent, () -> mustWaitForSmartMode = old);
    }

    @TestOnly
    @RequiredUIAccess
    public void runPasses(
        @Nonnull Application application,
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nonnull List<? extends TextEditor> textEditors,
        @Nonnull int[] toIgnore,
        boolean canChangeDocument,
        @Nullable Runnable callbackWhileWaiting
    ) throws ProcessCanceledException {
        assert myInitialized;
        assert !myDisposed;
        application.assertIsDispatchThread();
        if (application.isWriteAccessAllowed()) {
            throw new AssertionError("Must not start highlighting from within write action, or deadlock is imminent");
        }
        DaemonProgressIndicator.setDebug(!ApplicationInfoImpl.isInPerformanceTest());

        ((FileTypeManagerImpl)FileTypeManager.getInstance()).drainReDetectQueue();

        RefreshQueue refreshQueue = RefreshQueue.getInstance();
        do {
            UIUtil.dispatchAllInvocationEvents();
            // refresh will fire write actions interfering with highlighting
            // heavy ops are bad, but VFS refresh is ok
        }
        while (refreshQueue.isRefreshInProgress() || heavyProcessIsRunning());

        long dstart = System.currentTimeMillis();
        while (mustWaitForSmartMode && DumbService.getInstance(myProject).isDumb()) {
            if (System.currentTimeMillis() > dstart + 100000) {
                throw new IllegalStateException(
                    "Timeout waiting for smart mode." +
                        " If you absolutely want to be dumb, please use DaemonCodeAnalyzerImpl.mustWaitForSmartMode(false)."
                );
            }
            UIUtil.dispatchAllInvocationEvents();
        }

        UIUtil.dispatchAllInvocationEvents();

        FileStatusMapImpl fileStatusMap = getFileStatusMap();

        NonBlockingReadActionImpl.waitForAsyncTaskCompletion(application); // wait for async editor loading
        Map<FileEditor, HighlightingPass[]> map = new HashMap<>();
        for (TextEditor textEditor : textEditors) {
            TextEditorBackgroundHighlighter highlighter = (TextEditorBackgroundHighlighter)textEditor.getBackgroundHighlighter();
            if (highlighter == null) {
                Editor editor = textEditor.getEditor();
                throw new RuntimeException("Null highlighter from " + textEditor + "; loaded: " + AsyncEditorLoader.isEditorLoaded(editor));
            }
            List<TextEditorHighlightingPass> passes = highlighter.getPasses(toIgnore);
            HighlightingPass[] array = passes.toArray(HighlightingPass.EMPTY_ARRAY);
            assert array.length != 0 : "Highlighting is disabled for the file " + file;
            map.put(textEditor, array);
        }
        for (int ignoreId : toIgnore) {
            fileStatusMap.markFileUpToDate(document, ignoreId);
        }

        myUpdateRunnableFuture.cancel(false);

        DaemonProgressIndicator progress = createUpdateProgress(map.keySet());
        myPassExecutorService.submitPasses(map, progress);
        try {
            fileStatusMap.allowDirt(canChangeDocument);
            long start = System.currentTimeMillis();
            while (progress.isRunning() && System.currentTimeMillis() < start + 10 * 60 * 1000) {
                wrap(() -> {
                    progress.checkCanceled();
                    if (callbackWhileWaiting != null) {
                        callbackWhileWaiting.run();
                    }
                    waitInOtherThread(50, canChangeDocument);
                    UIUtil.dispatchAllInvocationEvents();
                    Throwable savedException = PassExecutorService.getSavedException(progress);
                    if (savedException != null) {
                        throw savedException;
                    }
                });
            }
            if (progress.isRunning() && !progress.isCanceled()) {
                throw new RuntimeException("Highlighting still running after " +
                    (System.currentTimeMillis() - start) / 1000 +
                    " seconds." +
                    " Still submitted passes: " +
                    myPassExecutorService.getAllSubmittedPasses() +
                    " ForkJoinPool.commonPool(): " +
                    ForkJoinPool.commonPool() +
                    "\n" +
                    ", ForkJoinPool.commonPool() active thread count: " +
                    ForkJoinPool.commonPool().getActiveThreadCount() +
                    ", ForkJoinPool.commonPool() has queued submissions: " +
                    ForkJoinPool.commonPool().hasQueuedSubmissions() +
                    "\n" +
                    ThreadDumper.dumpThreadsToString());
            }

            HighlightingSessionImpl session =
                (HighlightingSessionImpl)HighlightingSessionImpl.getOrCreateHighlightingSession(file, progress, null);
            wrap(() -> {
                if (!waitInOtherThread(60000, canChangeDocument)) {
                    throw new TimeoutException("Unable to complete in 60s");
                }
                session.waitForHighlightInfosApplied();
            });
            UIUtil.dispatchAllInvocationEvents();
            UIUtil.dispatchAllInvocationEvents();
            assert progress.isCanceled() && progress.isDisposed();
        }
        finally {
            DaemonProgressIndicator.setDebug(false);
            fileStatusMap.allowDirt(true);
            waitForTermination();
        }
    }

    @TestOnly
    private boolean waitInOtherThread(int millis, boolean canChangeDocument) throws Throwable {
        Disposable disposable = Disposable.newDisposable();
        // last hope protection against PsiModificationTrackerImpl.incCounter() craziness (yes, Kotlin)
        myProject.getMessageBus().connect(disposable).subscribe(PsiModificationTrackerListener.class, () -> {
            throw new IllegalStateException("You must not perform PSI modifications from inside highlighting");
        });
        if (!canChangeDocument) {
            myProject.getMessageBus().connect(disposable).subscribe(DaemonListener.class, new DaemonListener() {
                @Override
                public void daemonCancelEventOccurred(@Nonnull String reason) {
                    throw new IllegalStateException("You must not cancel daemon inside highlighting test: " + reason);
                }
            });
        }

        try {
            Future<Boolean> future = myProject.getApplication().executeOnPooledThread(() -> {
                try {
                    return myPassExecutorService.waitFor(millis);
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            return future.get();
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    @TestOnly
    public void prepareForTest() {
        setUpdateByTimerEnabled(false);
        waitForTermination();
        clearReferences();
    }

    @TestOnly
    public void cleanupAfterTest() {
        if (myProject.isOpen()) {
            prepareForTest();
        }
    }

    @TestOnly
    public void waitForTermination() {
        myPassExecutorService.cancelAll(true);
    }

    @Override
    public void settingsChanged() {
        DaemonCodeAnalyzerSettings settings = DaemonCodeAnalyzerSettings.getInstance();
        if (settings.isCodeHighlightingChanged(myLastSettings)) {
            restart();
        }
        myLastSettings = ((DaemonCodeAnalyzerSettingsImpl)settings).clone();
    }

    @Override
    public synchronized void setUpdateByTimerEnabled(boolean value) {
        myUpdateByTimerEnabled = value;
        stopProcess(value, "Update by timer change");
    }

    private final AtomicInteger myDisableCount = new AtomicInteger();

    @Override
    @RequiredUIAccess
    public void disableUpdateByTimer(@Nonnull Disposable parentDisposable) {
        setUpdateByTimerEnabled(false);
        myDisableCount.incrementAndGet();
        myProject.getApplication().assertIsDispatchThread();

        Disposer.register(
            parentDisposable,
            () -> {
                if (myDisableCount.decrementAndGet() == 0) {
                    setUpdateByTimerEnabled(true);
                }
            }
        );
    }

    synchronized boolean isUpdateByTimerEnabled() {
        return myUpdateByTimerEnabled;
    }

    @Override
    @RequiredUIAccess
    public void setImportHintsEnabled(@Nonnull PsiFile file, boolean value) {
        VirtualFile vFile = file.getVirtualFile();
        if (value) {
            myDisabledHintsFiles.remove(vFile);
            stopProcess(true, "Import hints change");
        }
        else {
            myDisabledHintsFiles.add(vFile);
            HintManager.getInstance().hideAllHints();
        }
    }

    @Override
    public void resetImportHintsEnabledForProject() {
        myDisabledHintsFiles.clear();
    }

    @Override
    public void setHighlightingEnabled(@Nonnull PsiFile file, boolean value) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(file);
        if (value) {
            myDisabledHighlightingFiles.remove(virtualFile);
        }
        else {
            myDisabledHighlightingFiles.add(virtualFile);
        }
    }

    @Override
    public boolean isHighlightingAvailable(@Nullable PsiFile file) {
        if (file == null || !file.isPhysical()) {
            return false;
        }
        if (myDisabledHighlightingFiles.contains(PsiUtilCore.getVirtualFile(file))) {
            return false;
        }

        if (file instanceof PsiCompiledElement) {
            return false;
        }
        FileType fileType = file.getFileType();

        // To enable T.O.D.O. highlighting
        return !fileType.isBinary();
    }

    @Override
    public boolean isImportHintsEnabled(@Nonnull PsiFile file) {
        return isAutohintsAvailable(file) && !myDisabledHintsFiles.contains(file.getVirtualFile());
    }

    @Override
    public boolean isAutohintsAvailable(PsiFile file) {
        return isHighlightingAvailable(file) && !(file instanceof PsiCompiledElement);
    }

    @Nonnull
    @Override
    public ProgressIndicator createDaemonProgressIndicator() {
        return new DaemonProgressIndicator();
    }

    @Override
    public void restart() {
        doRestart();
    }

    // return true if the progress was really canceled
    boolean doRestart() {
        myFileStatusMap.markAllFilesDirty("Global restart");
        return stopProcess(true, "Global restart");
    }

    @Override
    @RequiredReadAction
    public void restart(@Nonnull PsiFile file) {
        Document document = myPsiDocumentManager.getCachedDocument(file);
        if (document == null) {
            return;
        }
        String reason = "Psi file restart: " + file.getName();
        myFileStatusMap.markFileScopeDirty(document, new TextRange(0, document.getTextLength()), file.getTextLength(), reason);
        stopProcess(true, reason);
    }

    @Nonnull
    public List<ProgressableTextEditorHighlightingPass> getPassesToShowProgressFor(@Nonnull Document document) {
        List<HighlightingPass> allPasses = myPassExecutorService.getAllSubmittedPasses();
        return allPasses.stream()
            .map(p -> p instanceof ProgressableTextEditorHighlightingPass highlightingPass ? highlightingPass : null)
            .filter(p -> p != null && p.getDocument() == document)
            .sorted(Comparator.comparingInt(TextEditorHighlightingPass::getId))
            .collect(Collectors.toList());
    }

    boolean isAllAnalysisFinished(@Nonnull PsiFile file) {
        if (myDisposed) {
            return false;
        }
        Document document = myPsiDocumentManager.getCachedDocument(file);
        return document != null
            && document.getModificationStamp() == file.getViewProvider().getModificationStamp()
            && myFileStatusMap.allDirtyScopesAreNull(document);
    }

    @Override
    public boolean isErrorAnalyzingFinished(@Nonnull PsiFile file) {
        if (myDisposed) {
            return false;
        }
        Document document = myPsiDocumentManager.getCachedDocument(file);
        return document != null
            && document.getModificationStamp() == file.getViewProvider().getModificationStamp()
            && myFileStatusMap.getFileDirtyScope(document, Pass.UPDATE_ALL) == null;
    }

    @Override
    @Nonnull
    public FileStatusMapImpl getFileStatusMap() {
        return myFileStatusMap;
    }

    public synchronized boolean isRunning() {
        return !myUpdateProgress.isCanceled();
    }

    @TestOnly
    @RequiredUIAccess
    public boolean isRunningOrPending() {
        myProject.getApplication().assertIsDispatchThread();
        return isRunning() || !myUpdateRunnableFuture.isDone() || GeneralHighlightingPass.isRestartPending();
    }

    // return true if the progress really was canceled
    synchronized boolean stopProcess(boolean toRestartAlarm, @Nonnull String reason) {
        boolean canceled = cancelUpdateProgress(toRestartAlarm, reason);
        // optimisation: this check is to avoid too many re-schedules in case of thousands of events spikes
        boolean restart = toRestartAlarm && !myDisposed && myInitialized;

        if (restart && myUpdateRunnableFuture.isDone()) {
            myUpdateRunnableFuture =
                myProject.getUIAccess().getScheduler().schedule(myUpdateRunnable, mySettings.AUTOREPARSE_DELAY, TimeUnit.MILLISECONDS);
        }

        return canceled;
    }

    // return true if the progress really was canceled
    private synchronized boolean cancelUpdateProgress(boolean toRestartAlarm, @Nonnull String reason) {
        DaemonProgressIndicator updateProgress = myUpdateProgress;
        if (myDisposed) {
            return false;
        }
        boolean wasCanceled = updateProgress.isCanceled();
        myPassExecutorService.cancelAll(false);
        if (!wasCanceled) {
            PassExecutorService.log(updateProgress, null, "Cancel", reason, toRestartAlarm);
            updateProgress.cancel();
            return true;
        }
        return false;
    }

    @RequiredReadAction
    static boolean processHighlightsNearOffset(
        @Nonnull Document document,
        @Nonnull Project project,
        @Nonnull HighlightSeverity minSeverity,
        int offset,
        boolean includeFixRange,
        @Nonnull Predicate<? super HighlightInfo> processor
    ) {
        return processHighlights(
            document,
            project,
            null,
            0,
            document.getTextLength(),
            info -> {
                if (!isOffsetInsideHighlightInfo(offset, info, includeFixRange)) {
                    return true;
                }

                int compare = info.getSeverity().compareTo(minSeverity);
                return compare < 0 || processor.test(info);
            }
        );
    }

    @Nullable
    @RequiredReadAction
    public HighlightInfoImpl findHighlightByOffset(@Nonnull Document document, int offset, boolean includeFixRange) {
        return findHighlightByOffset(document, offset, includeFixRange, HighlightSeverity.INFORMATION);
    }

    @Nullable
    @RequiredReadAction
    HighlightInfoImpl findHighlightByOffset(
        @Nonnull Document document,
        int offset,
        boolean includeFixRange,
        @Nonnull HighlightSeverity minSeverity
    ) {
        List<HighlightInfoImpl> foundInfoList = new SmartList<>();
        processHighlightsNearOffset(
            document,
            myProject,
            minSeverity,
            offset,
            includeFixRange,
            info -> {
                if (info.getSeverity() == HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY || info.getType() == HighlightInfoType.TODO) {
                    return true;
                }
                if (!foundInfoList.isEmpty()) {
                    HighlightInfo foundInfo = foundInfoList.get(0);
                    int compare = foundInfo.getSeverity().compareTo(info.getSeverity());
                    if (compare < 0) {
                        foundInfoList.clear();
                    }
                    else if (compare > 0) {
                        return true;
                    }
                }
                foundInfoList.add((HighlightInfoImpl)info);
                return true;
            }
        );

        if (foundInfoList.isEmpty()) {
            return null;
        }
        if (foundInfoList.size() == 1) {
            return foundInfoList.get(0);
        }
        return HighlightInfoComposite.create(foundInfoList);
    }

    private static boolean isOffsetInsideHighlightInfo(int offset, @Nonnull HighlightInfo info, boolean includeFixRange) {
        RangeHighlighterEx highlighter = (RangeHighlighterEx)info.getHighlighter();
        if (highlighter == null || !highlighter.isValid()) {
            return false;
        }
        int startOffset = highlighter.getStartOffset();
        int endOffset = highlighter.getEndOffset();
        if (startOffset <= offset && offset <= endOffset) {
            return true;
        }
        if (!includeFixRange) {
            return false;
        }
        RangeMarker fixMarker = ((HighlightInfoImpl)info).fixMarker;
        if (fixMarker != null) {  // null means its range is the same as highlighter
            if (!fixMarker.isValid()) {
                return false;
            }
            startOffset = fixMarker.getStartOffset();
            endOffset = fixMarker.getEndOffset();
            return startOffset <= offset && offset <= endOffset;
        }
        return false;
    }

    @Nonnull
    @RequiredUIAccess
    public static List<LineMarkerInfo<?>> getLineMarkers(@Nonnull Document document, @Nonnull Project project) {
        Application.get().assertIsDispatchThread();
        List<LineMarkerInfo<?>> result = new ArrayList<>();
        LineMarkersUtil.processLineMarkers(
            project,
            document,
            new TextRange(0, document.getTextLength()),
            -1,
            new CommonProcessors.CollectProcessor<>(result)
        );
        return result;
    }

    @Nullable
    public IntentionHintComponent getLastIntentionHint() {
        return (IntentionHintComponent)IntentionsUI.getInstance(myProject).getLastIntentionHint();
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");
        if (myDisabledHintsFiles.isEmpty()) {
            return state;
        }

        List<String> array = new SmartList<>();
        for (VirtualFile file : myDisabledHintsFiles) {
            if (file.isValid()) {
                array.add(file.getUrl());
            }
        }

        if (!array.isEmpty()) {
            Collections.sort(array);

            Element disableHintsElement = new Element(DISABLE_HINTS_TAG);
            state.addContent(disableHintsElement);
            for (String url : array) {
                disableHintsElement.addContent(new Element(FILE_TAG).setAttribute(URL_ATT, url));
            }
        }
        return state;
    }

    @Override
    public void loadState(@Nonnull Element state) {
        myDisabledHintsFiles.clear();

        Element element = state.getChild(DISABLE_HINTS_TAG);
        if (element != null) {
            for (Element e : element.getChildren(FILE_TAG)) {
                String url = e.getAttributeValue(URL_ATT);
                if (url != null) {
                    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                    if (file != null) {
                        myDisabledHintsFiles.add(file);
                    }
                }
            }
        }
    }

    // made this class static and fields cleareable to avoid leaks when this object stuck in invokeLater queue
    private static class UpdateRunnable implements Runnable {
        private Project myProject;

        private UpdateRunnable(@Nonnull Project project) {
            myProject = project;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            UIAccess.assertIsUIThread();
            Project project = myProject;
            DaemonCodeAnalyzerImpl dca;
            if (project == null || !project.isInitialized() || project.isDisposed() || PowerSaveMode.isEnabled() || (dca =
                (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project)).myDisposed) {
                return;
            }

            Application app = myProject.getApplication();

            Collection<FileEditor> activeEditors = dca.getSelectedEditors();
            boolean updateByTimerEnabled = dca.isUpdateByTimerEnabled();
            PassExecutorService.log(
                dca.getUpdateProgress(),
                null,
                "Update Runnable. myUpdateByTimerEnabled:",
                updateByTimerEnabled,
                " something disposed:",
                PowerSaveMode.isEnabled() || !myProject.isInitialized(),
                " activeEditors:",
                activeEditors
            );
            if (!updateByTimerEnabled) {
                return;
            }

            if (activeEditors.isEmpty()) {
                return;
            }

            if (app.isWriteAccessAllowed()) {
                // makes no sense to start from within write action, will cancel anyway
                // we'll restart when the write action finish
                return;
            }
            if (dca.myPsiDocumentManager.hasUncommitedDocuments()) {
                // restart when everything committed
                dca.myPsiDocumentManager.performLaterWhenAllCommitted(this);
                return;
            }
            if (RefResolveService.ENABLED && !RefResolveService.getInstance(myProject).isUpToDate() && RefResolveService.getInstance(
                myProject).getQueueSize() == 1) {
                return; // if the user have just typed in something, wait until the file is re-resolved
                // (or else it will blink like crazy since unused symbols calculation depends on resolve service)
            }

            Map<FileEditor, HighlightingPass[]> passes = new HashMap<>(activeEditors.size());
            for (FileEditor fileEditor : activeEditors) {
                if (fileEditor instanceof TextEditor textEditor && !AsyncEditorLoader.isEditorLoaded(textEditor.getEditor())) {
                    // make sure the highlighting is restarted when the editor is finally loaded, because otherwise some crazy things happen,
                    // for instance `FileEditor.getBackgroundHighlighter()` returning null, essentially stopping highlighting silently
                    AsyncEditorLoader.performWhenLoaded(textEditor.getEditor(), this);
                }

                BackgroundEditorHighlighter highlighter = fileEditor.getBackgroundHighlighter();
                if (highlighter != null) {
                    HighlightingPass[] highlightingPasses = highlighter.createPassesForEditor();
                    passes.put(fileEditor, highlightingPasses);
                }
            }

            // wait for heavy processing to stop, re-schedule daemon but not too soon
            boolean heavyProcessIsRunning = heavyProcessIsRunning();
            if (heavyProcessIsRunning) {
                boolean hasPasses = false;
                for (Map.Entry<FileEditor, HighlightingPass[]> entry : passes.entrySet()) {
                    HighlightingPass[] filtered =
                        Arrays.stream(entry.getValue()).filter(DumbService::isDumbAware).toArray(HighlightingPass[]::new);
                    entry.setValue(filtered);
                    hasPasses |= filtered.length != 0;
                }
                if (!hasPasses) {
                    HeavyProcessLatch.INSTANCE.queueExecuteOutOfHeavyProcess(() -> dca.stopProcess(
                        true,
                        "re-scheduled to execute after heavy processing finished"
                    ));
                    return;
                }
            }

            // cancel all after calling createPasses() since there are perverts {@link consulo.ide.impl.idea.util.xml.ui.DomUIFactoryImpl} who are changing PSI there
            dca.cancelUpdateProgress(true, "Cancel by alarm");
            dca.myUpdateRunnableFuture.cancel(false);
            DaemonProgressIndicator progress = dca.createUpdateProgress(passes.keySet());
            dca.myPassExecutorService.submitPasses(passes, progress);
        }

        private void clearFieldsOnDispose() {
            myProject = null;
        }
    }

    // return true if a heavy op is running
    private static boolean heavyProcessIsRunning() {
        // VFS refresh is OK
        return HeavyProcessLatch.INSTANCE.isRunningAnythingBut(HeavyProcessLatch.Type.Syncing);
    }

    @Nonnull
    private synchronized DaemonProgressIndicator createUpdateProgress(@Nonnull Collection<FileEditor> fileEditors) {
        DaemonProgressIndicator old = myUpdateProgress;
        if (!old.isCanceled()) {
            old.cancel();
        }
        DaemonProgressIndicator progress = new MyDaemonProgressIndicator(myProject, fileEditors);
        progress.setModalityProgress(null);
        progress.start();
        myProject.getMessageBus().syncPublisher(DaemonListener.class).daemonStarting(fileEditors);
        myUpdateProgress = progress;
        return progress;
    }

    private static class MyDaemonProgressIndicator extends DaemonProgressIndicator {
        private final Project myProject;
        private Collection<FileEditor> myFileEditors;

        MyDaemonProgressIndicator(@Nonnull Project project, @Nonnull Collection<FileEditor> fileEditors) {
            myFileEditors = fileEditors;
            myProject = project;
        }

        @Override
        public void stopIfRunning() {
            super.stopIfRunning();
            myProject.getMessageBus().syncPublisher(DaemonListener.class).daemonFinished(myFileEditors);
            myFileEditors = null;
            HighlightingSessionImpl.clearProgressIndicator(this);
        }
    }


    @Override
    public void autoImportReferenceAtCursor(@Nonnull Editor editor, @Nonnull PsiFile file) {
        for (ReferenceImporter importer : ReferenceImporter.EP_NAME.getExtensionList()) {
            if (importer.autoImportReferenceAtCursor(editor, file)) {
                break;
            }
        }
    }

    @TestOnly
    @Nonnull
    public synchronized DaemonProgressIndicator getUpdateProgress() {
        return myUpdateProgress;
    }

    @Nonnull
    @RequiredUIAccess
    private Collection<FileEditor> getSelectedEditors() {
        Application app = myProject.getApplication();
        app.assertIsDispatchThread();

        // editors in modal context
        EditorTracker editorTracker = EditorTracker.getInstance(myProject);// myProject.getServiceIfCreated(EditorTracker.class);
        List<Editor> editors = editorTracker == null ? Collections.emptyList() : editorTracker.getActiveEditors();
        Collection<FileEditor> activeTextEditors;
        if (editors.isEmpty()) {
            activeTextEditors = Collections.emptyList();
        }
        else {
            activeTextEditors = new HashSet<>(editors.size());
            for (Editor editor : editors) {
                if (editor.isDisposed()) {
                    continue;
                }
                TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
                activeTextEditors.add(textEditor);
            }
        }

        if (app.getCurrentModalityState() != ModalityStateImpl.NON_MODAL) {
            return activeTextEditors;
        }

        Collection<FileEditor> result = new HashSet<>();
        Collection<VirtualFile> files = new HashSet<>(activeTextEditors.size());
        if (!app.isUnitTestMode()) {
            // editors in tabs
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            for (FileEditor tabEditor : fileEditorManager.getSelectedEditors()) {
                if (!tabEditor.isValid()) {
                    continue;
                }
                VirtualFile file = fileEditorManager.getFile(tabEditor);
                if (file != null) {
                    files.add(file);
                }
                result.add(tabEditor);
            }
        }

        // do not duplicate documents
        if (!activeTextEditors.isEmpty()) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            for (FileEditor fileEditor : activeTextEditors) {
                VirtualFile file = fileEditorManager.getFile(fileEditor);
                if (file != null && files.contains(file)) {
                    continue;
                }
                result.add(fileEditor);
            }
        }
        return result;
    }

    @TestOnly
    private static void wrap(@Nonnull ThrowableRunnable<?> runnable) {
        try {
            runnable.run();
        }
        catch (RuntimeException | Error e) {
            throw e;
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
