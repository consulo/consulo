/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.event.ApplicationListener;
import consulo.application.event.ApplicationLoadListener;
import consulo.application.impl.internal.concurent.AppScheduledExecutorService;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.impl.internal.progress.ProgressResult;
import consulo.application.impl.internal.progress.ProgressRunner;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.application.internal.StartupProgress;
import consulo.application.impl.internal.store.IApplicationStore;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationInfo;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.application.progress.*;
import consulo.application.util.ApplicationUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.store.internal.IComponentStore;
import consulo.component.store.internal.StateStorageException;
import consulo.component.store.internal.StorableComponent;
import consulo.component.store.internal.StoreUtil;
import consulo.component.util.BuildNumber;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectEx;
import consulo.project.internal.ProjectManagerEx;
import consulo.proxy.EventDispatcher;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.Stack;
import consulo.util.io.FileUtil;
import consulo.util.lang.*;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public abstract class BaseApplication extends PlatformComponentManagerImpl implements ApplicationEx, ApplicationWithIntentWriteLock, StorableComponent {
    private class ReadAccessToken extends AccessToken {
        private final RWLock.ReadToken myReader;

        private ReadAccessToken() {
            myReader = myLock.startRead();
        }

        @Override
        public void finish() {
            myLock.endRead(myReader);
        }
    }

    private class WriteAccessToken extends AccessToken {
        @Nonnull
        private final Class<?> clazz;

        WriteAccessToken(@Nonnull Class<?> clazz) {
            this.clazz = clazz;
            startWrite(clazz);
            markThreadNameInStackTrace();
        }

        @Override
        public void finish() {
            try {
                endWrite(clazz);
            }
            finally {
                unmarkThreadNameInStackTrace();
            }
        }

        private void markThreadNameInStackTrace() {
            String id = id();

            if (id != null) {
                Thread thread = Thread.currentThread();
                thread.setName(thread.getName() + id);
            }
        }

        private void unmarkThreadNameInStackTrace() {
            String id = id();

            if (id != null) {
                Thread thread = Thread.currentThread();
                String name = thread.getName();
                name = StringUtil.replace(name, id, "");
                thread.setName(name);
            }
        }

        @Nullable
        private String id() {
            Class<?> aClass = getClass();
            String name = aClass.getName();
            name = name.substring(name.lastIndexOf('.') + 1);
            name = name.substring(name.lastIndexOf('$') + 1);
            if (!name.equals("AccessToken")) {
                return " [" + name + "]";
            }
            return null;
        }
    }

    private static final Logger LOG = Logger.getInstance(BaseApplication.class);

    private static final int ourDumpThreadsOnLongWriteActionWaiting = Integer.getInteger("dump.threads.on.long.write.action.waiting", 0);

    @Nonnull
    protected final SimpleReference<? extends StartupProgress> mySplashRef;

    protected final Disposable myLastDisposable = Disposable.newDisposable(); // will be disposed last
    private final EventDispatcher<ApplicationListener> myDispatcher = EventDispatcher.create(ApplicationListener.class);

    // FIXME [VISTALL] we need this?
    protected final Stack<Class> myWriteActionsStack = new Stack<>();

    private final long myStartTime;

    protected RWLock myLock;

    protected boolean myDoNotSave;
    private boolean myLoaded;
    private volatile boolean myWriteActionPending;

    protected int myWriteStackBase;

    protected boolean myGatherStatistics;

    private final AtomicBoolean mySaveSettingsIsInProgress = new AtomicBoolean(false);

    private ProgressManager myProgressManager;

    private final Supplier<SemVer> myVersionValue = LazyValue.notNull(() -> {
        ApplicationInfo instance = ApplicationInfo.getInstance();
        String majorVersion = instance.getMajorVersion();
        String minorVersion = instance.getMinorVersion();

        String version = majorVersion + "." + minorVersion + ".0";
        return SemVer.parseFromText(version);
    });

    public BaseApplication(@Nonnull ComponentBinding componentBinding, @Nonnull SimpleReference<? extends StartupProgress> splashRef) {
        super(null, "Application", ComponentScope.APPLICATION, componentBinding);
        mySplashRef = splashRef;
        myStartTime = System.currentTimeMillis();
    }

    @Override
    public void initNotLazyServices() {
        // reinit progress manager since, it can try call getInstance while application is disposed
        myProgressManager = getInjectingContainer().getInstance(ProgressManager.class);

        super.initNotLazyServices();
    }

    @Nonnull
    @Override
    public ProgressManager getProgressManager() {
        return myProgressManager;
    }

    @Override
    public void executeNonCancelableSection(@Nonnull Runnable runnable) {
        if (myProgressManager != null) {
            myProgressManager.executeNonCancelableSection(runnable);
        }
        else {
            runnable.run();
        }
    }

    @Nullable
    @Override
    public ProgressIndicatorProvider getProgressIndicatorProvider() {
        return myProgressManager;
    }

    @Override
    protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
        super.bootstrapInjectingContainer(builder);

        builder.bind(Application.class).to(this);
        builder.bind(ApplicationEx.class).to(this);
        builder.bind(ApplicationInfo.class).to(ApplicationInfo::getInstance);
        builder.bind(ContainerPathManager.class).to(ContainerPathManager::get);

        builder.bind(FileTypeRegistry.class).to(FileTypeManager::getInstance);
        builder.bind(ProgressIndicatorProvider.class).to(this::getProgressManager);
        builder.bind(EncodingRegistry.class).to(ApplicationEncodingManager::getInstance);
    }

    protected void fireApplicationExiting() {
        myDispatcher.getMulticaster().applicationExiting();
    }

    protected void fireBeforeWriteActionStart(@Nonnull Class action) {
        myDispatcher.getMulticaster().beforeWriteActionStart(action);
    }

    protected void fireWriteActionStarted(@Nonnull Class action) {
        myDispatcher.getMulticaster().writeActionStarted(action);
    }

    protected void fireWriteActionFinished(@Nonnull Class action) {
        myDispatcher.getMulticaster().writeActionFinished(action);
    }

    protected void fireAfterWriteActionFinished(@Nonnull Class action) {
        myDispatcher.getMulticaster().afterWriteActionFinished(action);
    }

    @Override
    public void load(@Nullable String optionsPath) throws IOException {
        load(ContainerPathManager.get().getConfigPath(), optionsPath == null ? ContainerPathManager.get().getOptionsPath() : optionsPath);
    }

    public void load(@Nonnull String configPath, @Nonnull String optionsPath) throws IOException {
        IApplicationStore store = getStateStore();
        store.setOptionsPath(optionsPath);
        store.setConfigPath(configPath);

        fireBeforeApplicationLoaded();

        try {
            store.load();
        }
        catch (StateStorageException e) {
            throw new IOException(e.getMessage());
        }

        myLoaded = true;

        createLocatorFile();
    }

    private static void createLocatorFile() {
        ContainerPathManager containerPathManager = ContainerPathManager.get();
        File locatorFile = new File(containerPathManager.getSystemPath() + "/" + ApplicationEx.LOCATOR_FILE_NAME);
        try {
            byte[] data = containerPathManager.getHomePath().getBytes(StandardCharsets.UTF_8);
            FileUtil.writeToFile(locatorFile, data);
        }
        catch (IOException e) {
            LOG.warn("can't store a location in '" + locatorFile + "'", e);
        }
    }

    private void fireBeforeApplicationLoaded() {
        getExtensionPoint(ApplicationLoadListener.class).forEachExtensionSafe(ApplicationLoadListener::beforeApplicationLoaded);
    }

    @Override
    public void saveSettings() {
        if (myDoNotSave) {
            return;
        }
        _saveSettings();
    }

    // public for testing purposes
    public void _saveSettings() {
        if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
            try {
                StoreUtil.save(getStateStore(), false, null);
            }
            finally {
                mySaveSettingsIsInProgress.set(false);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void saveAll() {
        if (myDoNotSave) {
            return;
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project openProject : openProjects) {
            if (openProject.isDisposed()) {
                // debug for https://github.com/consulo/consulo/issues/296
                LOG.error("Project is disposed: " + openProject.getName() + ", isInitialized: " + openProject.isInitialized());
                continue;
            }

            ProjectEx project = (ProjectEx)openProject;
            if (project.isInitialized()) {
                project.save();
            }
        }

        saveSettings();
    }

    @Override
    public void doNotSave(boolean value) {
        myDoNotSave = value;
    }

    @Override
    public boolean isDoNotSave() {
        return myDoNotSave;
    }

    @Override
    public boolean isLoaded() {
        return myLoaded;
    }

    @Nonnull
    @Override
    public Future<?> executeOnPooledThread(@Nonnull final Runnable action) {
        return getInstance(ApplicationConcurrency.class).getExecutorService().submit(new RunnableAsCallable(action, LOG));
    }

    @Nonnull
    @Override
    public <T> Future<T> executeOnPooledThread(@Nonnull final Callable<T> action) {
        return getInstance(ApplicationConcurrency.class).getExecutorService().submit(new Callable<T>() {
            @Override
            public T call() {
                try {
                    return action.call();
                }
                catch (ProcessCanceledException e) {
                    // ignore
                }
                catch (Throwable t) {
                    if (!(t instanceof ProcessCanceledException)) {
                        LOG.error(t);
                    }
                }
                finally {
                    Thread.interrupted(); // reset interrupted status
                }
                return null;
            }

            @Override
            public String toString() {
                return action.toString();
            }
        });
    }

    @Override
    public long getStartTime() {
        return myStartTime;
    }

    @Override
    public void addApplicationListener(@Nonnull ApplicationListener l) {
        myDispatcher.addListener(l);
    }

    @Override
    public void addApplicationListener(@Nonnull ApplicationListener l, @Nonnull Disposable parent) {
        myDispatcher.addListener(l, parent);
    }

    @Override
    public void removeApplicationListener(@Nonnull ApplicationListener l) {
        myDispatcher.removeListener(l);
    }

    protected boolean canExit() {
        ProjectManagerEx projectManager = (ProjectManagerEx)ProjectManager.getInstance();
        Project[] projects = projectManager.getOpenProjects();
        for (Project project : projects) {
            if (!projectManager.canClose(project)) {
                return false;
            }
        }

        return true;
    }

    @RequiredUIAccess
    @Override
    public void dispose() {
        assertIsDispatchThread();

        fireApplicationExiting();

        ShutDownTracker.getInstance().ensureStopperThreadsFinished();

        ApplicationConcurrency concurrency = getInstance(ApplicationConcurrency.class);

        super.dispose();

        invokeLater(this::releaseWriteIntentLock, ModalityState.nonModal());

        AppScheduledExecutorService service = (AppScheduledExecutorService)concurrency.getScheduledExecutorService();
        service.shutdownAppScheduledExecutorService();

        Disposer.dispose(myLastDisposable); // dispose it last
    }

    @Override
    protected void notifyAboutInitialization(float percentOfLoad, Object component) {
        StartupProgress progress = mySplashRef.get();
        if (progress != null) {
            float progress1 = 0.65f + percentOfLoad * 0.35f;
            progress.showProgress("", progress1);
        }
    }

    @Nonnull
    @Override
    public IApplicationStore getStateStore() {
        return (IApplicationStore)super.getStateStore();
    }

    @Override
    @Nonnull
    public IComponentStore getStateStoreImpl() {
        return getInstance(IApplicationStore.class);
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return ApplicationProperties.isInSandbox() ? PlatformIconGroup.icon16_sandbox() : PlatformIconGroup.icon16();
    }

    @Nonnull
    @Override
    public Image getBigIcon() {
        return ApplicationProperties.isInSandbox() ? PlatformIconGroup.icon32_sandbox() : PlatformIconGroup.icon32();
    }

    @Nonnull
    @Override
    public SemVer getVersion() {
        return myVersionValue.get();
    }

    @Nonnull
    @Override
    public BuildNumber getBuildNumber() {
        return ApplicationInfo.getInstance().getBuild();
    }

    @Nonnull
    @Override
    public AccessToken acquireReadActionLock() {
        DeprecatedMethodException.report("Use runReadAction() instead");

        // if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
        return isWriteThread() || myLock.isReadLockedByThisThread() ? AccessToken.EMPTY_ACCESS_TOKEN : new ReadAccessToken();
    }

    @Override
    public void runReadAction(@Nonnull final Runnable action) {
        RWLock.ReadToken status = myLock.startRead();
        try {
            action.run();
        }
        finally {
            if (status != null) {
                myLock.endRead(status);
            }
        }
    }

    @Override
    public <T> T runReadAction(@Nonnull final Supplier<T> computation) {
        RWLock.ReadToken status = myLock.startRead();
        try {
            return computation.get();
        }
        finally {
            if (status != null) {
                myLock.endRead(status);
            }
        }
    }

    @Override
    public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableSupplier<T, E> computation) throws E {
        RWLock.ReadToken status = myLock.startRead();
        try {
            return computation.get();
        }
        finally {
            if (status != null) {
                myLock.endRead(status);
            }
        }
    }

    @Override
    public boolean tryRunReadAction(@Nonnull Runnable action) {
        //if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
        RWLock.ReadToken status = myLock.startTryRead();
        if (status != null && !status.readRequested()) {
            return false;
        }
        try {
            action.run();
        }
        finally {
            if (status != null) {
                myLock.endRead(status);
            }
        }
        return true;
    }


    @RequiredUIAccess
    @Override
    public void executeSuspendingWriteAction(@Nullable ComponentManager project, @Nonnull String title, @Nonnull Runnable runnable) {
        assertIsWriteThread();
        if (!myLock.isWriteLocked()) {
            runModalProgress(project, title, runnable);
            return;
        }

        int prevBase = myWriteStackBase;
        myWriteStackBase = myWriteActionsStack.size();
        try (AccessToken ignored = myLock.writeSuspend()) {
            runModalProgress(project, title, runnable);
        }
        finally {
            myWriteStackBase = prevBase;
        }
    }

    private static void runModalProgress(@Nullable ComponentManager project, @Nonnull String title, @Nonnull Runnable runnable) {
        new Task.Modal(project, title, false) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                runnable.run();
            }
        }.queue();
    }

    @Override
    public boolean isReadAccessAllowed() {
        return isWriteThread() || myLock.isReadLockedByThisThread() || isDispatchThread();
    }

    @Override
    public boolean isInImpatientReader() {
        return myLock.isInImpatientReader();
    }

    @Override
    public boolean isWriteActionPending() {
        return myWriteActionPending;
    }

    @RequiredWriteAction
    @Override
    public void assertWriteAccessAllowed() {
        LOG.assertTrue(
            isWriteAccessAllowed(),
            "Write access is allowed inside write-action only (see consulo.ide.impl.idea.openapi.application.Application.runWriteAction())"
        );
    }

    @Override
    public boolean holdsReadLock() {
        return myLock.isReadLockedByThisThread();
    }

    /**
     * Executes a {@code runnable} in an "impatient" mode.
     * In this mode any attempt to call {@link #runReadAction(Runnable)}
     * would fail (i.e. throw {@link ApplicationUtil.CannotRunReadActionException})
     * if there is a pending write action.
     */
    @Override
    public void executeByImpatientReader(@RequiredReadAction @Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
        if (isDispatchThread()) {
            runnable.run();
        }
        else {
            myLock.executeByImpatientReader(runnable);
        }
    }

    @Override
    public boolean isWriteActionInProgress() {
        return myLock.isWriteLocked();
    }

    @RequiredUIAccess
    @Override
    public boolean runProcessWithProgressSynchronously(
        @Nonnull final Runnable process,
        @Nonnull final String progressTitle,
        final boolean canBeCanceled,
        boolean shouldShowModalWindow,
        @Nullable final ComponentManager project,
        final JComponent parentComponent,
        @Nonnull LocalizeValue cancelText
    ) {
        if (isDispatchThread() && isWriteAccessAllowed()
            // Disallow running process in separate thread from under write action.
            // The thread will deadlock trying to get read action otherwise.
        ) {
            LOG.debug("Starting process with progress from within write action makes no sense");
            try {
                ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
            }
            catch (ProcessCanceledException e) {
                // ok to ignore.
                return false;
            }
            return true;
        }

        CompletableFuture<ProgressWindow> progress = createProgressWindowAsyncIfNeeded(
            progressTitle,
            canBeCanceled,
            shouldShowModalWindow,
            project,
            parentComponent,
            cancelText
        );

        ProgressRunner<?> progressRunner = new ProgressRunner<>(process)
            .sync()
            .modal()
            .withProgress(progress);

        ProgressResult<?> result = progressRunner.submitAndGet();

        Throwable exception = result.getThrowable();
        if (!(exception instanceof ProcessCanceledException)) {
            ExceptionUtil.rethrowUnchecked(exception);
        }
        return !result.isCanceled();
    }

    @Nonnull
    public final CompletableFuture<ProgressWindow> createProgressWindowAsyncIfNeeded(
        @Nonnull String progressTitle,
        boolean canBeCanceled,
        boolean shouldShowModalWindow,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        @Nonnull LocalizeValue cancelText
    ) {
        if (UIAccess.isUIThread()) {
            return CompletableFuture.completedFuture(createProgressWindow(
                progressTitle,
                canBeCanceled,
                shouldShowModalWindow,
                project,
                parentComponent,
                cancelText
            ));
        }
        return CompletableFuture.supplyAsync(
            () -> createProgressWindow(
                progressTitle,
                canBeCanceled,
                shouldShowModalWindow,
                project,
                parentComponent,
                cancelText
            ),
            this::invokeLater
        );
    }

    @Nonnull
    public ProgressWindow createProgressWindow(
        @Nonnull String progressTitle,
        boolean canBeCanceled,
        boolean shouldShowModalWindow,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        @Nonnull LocalizeValue cancelText
    ) {
        ProgressWindow progress = new ProgressWindow(canBeCanceled, !shouldShowModalWindow, (Project) project, parentComponent, cancelText);
        // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
        // does not have a chance to run, and as a result the progress won't be disposed
        Disposer.register(this, progress);
        progress.setTitle(progressTitle);
        return progress;
    }

    protected void startWrite(@Nonnull Class clazz) {
        assertIsWriteThread();
        boolean writeActionPending = myWriteActionPending;
        myWriteActionPending = true;
        try {
            ActivityTracker.getInstance().inc();
            fireBeforeWriteActionStart(clazz);

            if (!myLock.isWriteLocked()) {
                int delay = ourDumpThreadsOnLongWriteActionWaiting;
                Future<?> reportSlowWrite = delay <= 0
                    ? null
                    : AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                        () -> PerformanceWatcher.getInstance().dumpThreads("waiting", true),
                        delay,
                        delay,
                        TimeUnit.MILLISECONDS
                    );
                long t = LOG.isDebugEnabled() ? System.currentTimeMillis() : 0;
                myLock.writeLock();
                if (LOG.isDebugEnabled()) {
                    long elapsed = System.currentTimeMillis() - t;
                    if (elapsed != 0) {
                        LOG.debug("Write action wait time: " + elapsed);
                    }
                }
                if (reportSlowWrite != null) {
                    reportSlowWrite.cancel(false);
                }
            }
        }
        finally {
            myWriteActionPending = writeActionPending;
        }

        myWriteActionsStack.push(clazz);
        fireWriteActionStarted(clazz);
    }

    protected void endWrite(Class clazz) {
        try {
            fireWriteActionFinished(clazz);
            // fire listeners before popping stack because if somebody starts write action in a listener,
            // there is a danger of unlocking the write lock before other listeners have been run (since write lock became non-reentrant).
        }
        finally {
            myWriteActionsStack.pop();
            if (myWriteActionsStack.size() == myWriteStackBase) {
                myLock.writeUnlock();
            }
            if (myWriteActionsStack.isEmpty()) {
                fireAfterWriteActionFinished(clazz);
            }
        }
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public AccessToken acquireWriteActionLock(@Nonnull Class clazz) {
        DeprecatedMethodException.report("Use runWriteAction() instead");

        return new WriteAccessToken(clazz);
    }

    @RequiredUIAccess
    @Override
    public void runWriteAction(@Nonnull final Runnable action) {
        Class<? extends Runnable> clazz = action.getClass();
        startWrite(clazz);
        try {
            action.run();
        }
        finally {
            endWrite(clazz);
        }
    }

    @RequiredUIAccess
    @Override
    public <T> T runWriteAction(@Nonnull final Supplier<T> computation) {
        return runWriteActionWithClass(computation.getClass(), computation::get);
    }

    @RequiredUIAccess
    @Override
    public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableSupplier<T, E> computation) throws E {
        return runWriteActionWithClass(computation.getClass(), computation);
    }

    @Override
    @RequiredReadAction
    public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
        assertReadAccessAllowed();

        for (int i = myWriteActionsStack.size() - 1; i >= 0; i--) {
            Class<?> action = myWriteActionsStack.get(i);
            if (actionClass == action || ReflectionUtil.isAssignable(actionClass, action)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isWriteThread() {
        return myLock.isWriteThread();
    }

    protected <T, E extends Throwable> T runWriteActionWithClass(
        @Nonnull Class<?> clazz,
        @Nonnull ThrowableSupplier<T, E> computable
    ) throws E {
        startWrite(clazz);
        try {
            return computable.get();
        }
        finally {
            endWrite(clazz);
        }
    }

    @Override
    public void acquireWriteIntentLock(@Nonnull String invokedClassFqn) {
        myLock.writeIntentLock();
    }

    @Override
    public void releaseWriteIntentLock() {
        myLock.writeIntentUnlock();
    }

    @Override
    public void invokeLaterOnWriteThread(@Nonnull Runnable action, @Nonnull ModalityState modal) {
        invokeLaterOnWriteThread(action, modal, getDisposed());
    }

    @Override
    public void invokeLaterOnWriteThread(
        @Nonnull Runnable action,
        @Nonnull ModalityState modal,
        @Nonnull BooleanSupplier expired
    ) {
        Runnable r = wrapLaterInvocation(action, modal);
        // EDT == Write Thread in legacy mode
        LaterInvocator.invokeLaterWithCallback(
            () -> runIntendedWriteActionOnCurrentThread(r),
            modal,
            expired,
            null
        );
    }

    @Nonnull
    protected Runnable wrapLaterInvocation(Runnable action, ModalityState state) {
        return action;
    }

    @Override
    public <T, E extends Throwable> T runUnlockingIntendedWrite(@Nonnull ThrowableComputable<T, E> action) throws E {
        return action.compute();
    }

    @Override
    public void runIntendedWriteActionOnCurrentThread(@Nonnull Runnable action) {
        if (isWriteThread()) {
            action.run();
        }
        else {
            acquireWriteIntentLock(action.getClass().getName());
            try {
                action.run();
            }
            finally {
                releaseWriteIntentLock();
            }
        }
    }

    @Override
    public void invokeLaterOnWriteThread(@Nonnull Runnable action) {
        invokeLaterOnWriteThread(action, getDefaultModalityState());
    }

    @Override
    public boolean isWriteAccessAllowed() {
        return isWriteThread() && myLock.isWriteLocked();
    }

    @Override
    public boolean isDispatchThread() {
        return UIAccess.isUIThread();
    }

    public boolean isCurrentWriteOnUIThread() {
        return false;
    }

    @Override
    public String toString() {
        return "Application" +
            (isDisposed() ? " (Disposed)" : "") +
            (ApplicationProperties.isInSandbox() ? " (Sandbox)" : "") +
            (isHeadlessEnvironment() ? " (Headless)" : "") +
            (isCommandLine() ? " (Command line)" : "");
    }
}
