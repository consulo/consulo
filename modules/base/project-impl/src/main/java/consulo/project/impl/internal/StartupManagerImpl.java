// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.internal.perfomance.Activity;
import consulo.application.internal.perfomance.ActivityCategory;
import consulo.application.internal.perfomance.StartUpMeasurer;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.internal.StartupManagerEx;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.startup.PostStartupActivity;
import consulo.project.startup.StartupActivity;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@ServiceImpl
public class StartupManagerImpl extends StartupManagerEx implements Disposable {
    private static final Logger LOG = Logger.getInstance(StartupManagerImpl.class);
    private static final long EDT_WARN_THRESHOLD_IN_NANO = TimeUnit.MILLISECONDS.toNanos(100);

    private final Object myLock = new Object();

    private final Deque<StartupActivity> myPreStartupActivities = new ArrayDeque<>();
    private final Deque<StartupActivity> myStartupActivities = new ArrayDeque<>();

    private final Deque<StartupActivity> myDumbAwarePostStartupActivities = new ArrayDeque<>();
    private final Deque<StartupActivity> myNotDumbAwarePostStartupActivities = new ArrayDeque<>();
    // guarded by this
    private boolean myPostStartupActivitiesPassed;

    private volatile boolean myPreStartupActivitiesPassed;
    private volatile boolean myStartupActivitiesPassed;

    @Nonnull
    private final Application myApplication;
    private final Project myProject;
    private ScheduledFuture<?> myBackgroundPostStartupScheduledFuture;

    @Inject
    public StartupManagerImpl(@Nonnull Application application, @Nonnull Project project) {
        myApplication = application;
        myProject = project;
    }

    private void checkNonDefaultProject() {
        LOG.assertTrue(!myProject.isDefault(), "Please don't register startup activities for the default project: they won't ever be run");
    }

    @Override
    public void registerPreStartupActivity(@Nonnull StartupActivity runnable) {
        checkNonDefaultProject();
        LOG.assertTrue(!myPreStartupActivitiesPassed, "Registering pre startup activity that will never be run");
        synchronized (myLock) {
            myPreStartupActivities.add(runnable);
        }
    }

    @Override
    public void registerStartupActivity(@Nonnull StartupActivity runnable) {
        checkNonDefaultProject();
        LOG.assertTrue(!myStartupActivitiesPassed, "Registering startup activity that will never be run");
        synchronized (myLock) {
            myStartupActivities.add(runnable);
        }
    }

    @Override
    public synchronized void registerPostStartupActivity(@Nonnull StartupActivity consumer) {
        checkNonDefaultProject();
        if (myPostStartupActivitiesPassed) {
            LOG.error("Registering post-startup activity that will never be run:" + " disposed=" + myProject.isDisposed() + "; open=" + myProject.isOpen() + "; passed=" + myStartupActivitiesPassed);
        }

        Deque<StartupActivity> list = DumbService.isDumbAware(consumer) ? myDumbAwarePostStartupActivities : myNotDumbAwarePostStartupActivities;
        synchronized (myLock) {
            list.add(consumer);
        }
    }

    @Override
    public boolean startupActivityPassed() {
        return myStartupActivitiesPassed;
    }

    @Override
    public synchronized boolean postStartupActivityPassed() {
        return myPostStartupActivitiesPassed;
    }

    @SuppressWarnings("SynchronizeOnThis")
    public void runStartupActivities(UIAccess uiAccess) {
        myApplication.runReadAction(() -> {
            runActivities(uiAccess, myPreStartupActivities, StartUpMeasurer.Phases.PROJECT_PRE_STARTUP);

            // to avoid atomicity issues if runWhenProjectIsInitialized() is run at the same time
            synchronized (this) {
                myPreStartupActivitiesPassed = true;
            }

            runActivities(uiAccess, myStartupActivities, StartUpMeasurer.Phases.PROJECT_STARTUP);

            synchronized (this) {
                myStartupActivitiesPassed = true;
            }
        });
    }

    public void runPostStartupActivitiesFromExtensions(UIAccess uiAccess) {
        PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
        // strictly speaking, the activity is not sequential, because sub-activities are performed in different threads
        // (depending on dumb-awareness), but because there is no other concurrent phase and timeline end equals to last dumb-aware activity,
        // we measure it as a sequential activity to put it on the timeline and make clear what's going on the end (avoid last "unknown" phase)
        Activity dumbAwareActivity = StartUpMeasurer.startMainActivity("project post-startup dumb-aware activities");

        AtomicReference<Activity> edtActivity = new AtomicReference<>();

        AtomicBoolean uiFreezeWarned = new AtomicBoolean();
        DumbService dumbService = DumbService.getInstance(myProject);

        AtomicInteger counter = new AtomicInteger();
        myApplication.getExtensionPoint(PostStartupActivity.class).processWithPluginDescriptor((extension, pluginDescriptor) -> {
            if (DumbService.isDumbAware(extension)) {
                runActivity(uiFreezeWarned, uiAccess, extension, pluginDescriptor);
            }
            else {
                if (edtActivity.get() == null) {
                    edtActivity.set(StartUpMeasurer.startMainActivity("project post-startup edt activities"));
                }

                counter.incrementAndGet();
                dumbService.runWhenSmart(() -> {
                    runActivity(uiFreezeWarned, uiAccess, extension, pluginDescriptor);
                    if (counter.decrementAndGet() == 0) {
                        Activity activity = edtActivity.getAndSet(null);
                        if (activity != null) {
                            activity.end();
                        }
                    }
                });
            }
        });

        if (counter.get() == 0) {
            Activity activity = edtActivity.getAndSet(null);
            if (activity != null) {
                activity.end();
            }
        }
        dumbAwareActivity.end();
        snapshot.logResponsivenessSinceCreation("Post-startup activities under progress");

        //StartupActivity.POST_STARTUP_ACTIVITY.addExtensionPointListener(new ExtensionPointListener<StartupActivity>() {
        //  @Override
        //  public void extensionAdded(@Nonnull StartupActivity extension, @Nonnull PluginDescriptor pluginDescriptor) {
        //    if (DumbService.isDumbAware(extension)) {
        //      runActivity(new AtomicBoolean(), extension, pluginDescriptor);
        //    }
        //    else {
        //      dumbService.runWhenSmart(() -> runActivity(new AtomicBoolean(), extension, pluginDescriptor));
        //    }
        //  }
        //}, this);
    }

    private void runActivity(@Nonnull AtomicBoolean uiFreezeWarned, @Nonnull UIAccess uiAccess, @Nonnull StartupActivity extension, @Nonnull PluginDescriptor pluginDescriptor) {
        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        if (indicator != null) {
            indicator.pushState();
        }
        long startTime = StartUpMeasurer.getCurrentTime();
        try {
            extension.runActivity(myProject, uiAccess);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
        }
        finally {
            if (indicator != null) {
                indicator.popState();
            }
        }

        String pluginId = pluginDescriptor.getPluginId().getIdString();
        long duration = StartUpMeasurer.addCompletedActivity(startTime, extension.getClass(), ActivityCategory.POST_STARTUP_ACTIVITY, pluginId, StartUpMeasurer.MEASURE_THRESHOLD);
        if (duration > EDT_WARN_THRESHOLD_IN_NANO) {
            reportUiFreeze(uiFreezeWarned);
        }
    }

    private void reportUiFreeze(@Nonnull AtomicBoolean uiFreezeWarned) {
        Application app = myApplication;
        if (!app.isUnitTestMode() && app.isDispatchThread() && uiFreezeWarned.compareAndSet(false, true)) {
            LOG.info("Some post-startup activities freeze UI for noticeable time. Please consider making them DumbAware to run them in background" +
                " under modal progress, or just making them faster to speed up project opening.");
        }
    }

    public void runPostStartupActivities(UIAccess uiAccess) {
        if (postStartupActivityPassed()) {
            return;
        }

        final Application app = myApplication;

        runActivities(uiAccess, myDumbAwarePostStartupActivities, StartUpMeasurer.Phases.PROJECT_DUMB_POST_STARTUP);

        DumbService dumbService = DumbService.getInstance(myProject);
        dumbService.runWhenSmart(new Runnable() {
            @Override
            @RequiredUIAccess
            public void run() {
                app.assertIsDispatchThread();

                // myDumbAwarePostStartupActivities might be non-empty if new activities were registered during dumb mode
                runActivities(uiAccess, myDumbAwarePostStartupActivities, StartUpMeasurer.Phases.PROJECT_DUMB_POST_STARTUP);

                while (true) {
                    List<StartupActivity> dumbUnaware = takeDumbUnawareStartupActivities();
                    if (dumbUnaware.isEmpty()) {
                        break;
                    }

                    // queue each activity in smart mode separately so that if one of them starts the dumb mode, the next ones just wait for it to finish
                    for (StartupActivity activity : dumbUnaware) {
                        dumbService.runWhenSmart(() -> runActivity(uiAccess, activity));
                    }
                }

                if (dumbService.isDumb()) {
                    // return here later to process newly submitted activities (if any) and set myPostStartupActivitiesPassed
                    dumbService.runWhenSmart(this);
                }
                else {
                    //noinspection SynchronizeOnThis
                    synchronized (this) {
                        myPostStartupActivitiesPassed = true;
                    }
                }
            }
        });
    }

    @Nonnull
    private List<StartupActivity> takeDumbUnawareStartupActivities() {
        synchronized (myLock) {
            if (myNotDumbAwarePostStartupActivities.isEmpty()) {
                return List.of();
            }

            List<StartupActivity> result = new ArrayList<>(myNotDumbAwarePostStartupActivities);
            myNotDumbAwarePostStartupActivities.clear();
            return result;
        }
    }


    private void runActivities(@Nonnull UIAccess uiAccess, @Nonnull Deque<StartupActivity> activities, @Nonnull String phaseName) {
        Activity activity = StartUpMeasurer.startMainActivity(phaseName);

        while (true) {
            StartupActivity startupActivity;
            synchronized (myLock) {
                startupActivity = activities.pollFirst();
            }

            if (startupActivity == null) {
                break;
            }

            long startTime = StartUpMeasurer.getCurrentTime();

            PluginDescriptor plugin = PluginManager.getPlugin(startupActivity.getClass());
            PluginId pluginId = plugin != null ? plugin.getPluginId() : PluginIds.CONSULO_BASE;

            runActivity(uiAccess, startupActivity);

            StartUpMeasurer.addCompletedActivity(startTime, startupActivity.getClass(), ActivityCategory.POST_STARTUP_ACTIVITY, pluginId.toString(), StartUpMeasurer.MEASURE_THRESHOLD);
        }

        activity.end();
    }

    public final void scheduleBackgroundPostStartupActivities(@Nonnull UIAccess uiAccess) {
        if (myProject.isDisposedOrDisposeInProgress()) {
            return;
        }

        myBackgroundPostStartupScheduledFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (myProject.isDisposedOrDisposeInProgress()) {
                return;
            }

            List<BackgroundStartupActivity> activities = myApplication.getExtensionPoint(BackgroundStartupActivity.class).getExtensionList();
            //StartupActivity.BACKGROUND_POST_STARTUP_ACTIVITY.addExtensionPointListener(new ExtensionPointListener<StartupActivity.Background>() {
            //  @Override
            //  public void extensionAdded(@Nonnull StartupActivity.Background extension, @Nonnull PluginDescriptor pluginDescriptor) {
            //    extension.runActivity(myProject);
            //  }
            //}, this);

            BackgroundTaskUtil.runUnderDisposeAwareIndicator(this, () -> {
                for (StartupActivity activity : activities) {
                    ProgressManager.checkCanceled();

                    if (myProject.isDisposedOrDisposeInProgress()) {
                        return;
                    }

                    activity.runActivity(myProject, uiAccess);
                }
            });
        }, Registry.intValue("ide.background.post.startup.activity.delay"), TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        if (myBackgroundPostStartupScheduledFuture != null) {
            myBackgroundPostStartupScheduledFuture.cancel(false);
        }
    }

    private void runActivity(@Nonnull UIAccess uiAccess, @Nonnull StartupActivity startupActivity) {
        ProgressManager.checkCanceled();
        try {
            startupActivity.runActivity(myProject, uiAccess);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable ex) {
            LOG.error(ex);
        }
    }

    @Override
    public void runWhenProjectIsInitialized(@Nonnull StartupActivity startupActivity) {
        final Application application = myProject.getApplication();

        Runnable runnable = () -> {
            if (myProject.isDisposedOrDisposeInProgress()) {
                return;
            }

            //noinspection SynchronizeOnThis
            synchronized (this) {
                // in tests that simulate project opening, post-startup activities could have been run already
                // then we should act as if the project was initialized
                boolean initialized = myProject.isInitialized() || myProject.isDefault() || (myPostStartupActivitiesPassed && application.isUnitTestMode());
                if (!initialized) {
                    registerPostStartupActivity(startupActivity);
                    return;
                }
            }

            startupActivity.runActivity(myProject, UIAccess.current());
        };
        if (application.isDispatchThread()) {
            runnable.run();
        }
        else {
            application.invokeLater(runnable, application.getDefaultModalityState());
        }
    }

    @Override
    public void runAfterOpened(@Nonnull StartupActivity startupActivity) {
        checkNonDefaultProject();

        if (!myPostStartupActivitiesPassed) {
            synchronized (myLock) {
                if (!myPostStartupActivitiesPassed) {
                    registerPostStartupActivity(startupActivity);
                    return;
                }
            }
        }

        startupActivity.runActivity(myProject, myApplication.getLastUIAccess());
    }

    @TestOnly
    public synchronized void prepareForNextTest() {
        synchronized (myLock) {
            myPreStartupActivities.clear();
            myStartupActivities.clear();
            myDumbAwarePostStartupActivities.clear();
            myNotDumbAwarePostStartupActivities.clear();
        }
    }

    @TestOnly
    public synchronized void checkCleared() {
        try {
            synchronized (myLock) {
                assert myStartupActivities.isEmpty() : "Activities: " + myStartupActivities;
                assert myDumbAwarePostStartupActivities.isEmpty() : "DumbAware Post Activities: " + myDumbAwarePostStartupActivities;
                assert myNotDumbAwarePostStartupActivities.isEmpty() : "Post Activities: " + myNotDumbAwarePostStartupActivities;
                assert myPreStartupActivities.isEmpty() : "Pre Activities: " + myPreStartupActivities;
            }
        }
        finally {
            prepareForNextTest();
        }
    }
}