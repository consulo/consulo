// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.*;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionList;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointName;
import consulo.component.util.ComponentUtil;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.event.DumbModeListener;
import consulo.ui.ModalityState;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A service managing the IDE's 'dumb' mode: when indexes are updated in the background, and the functionality is very much limited.
 * Only the explicitly allowed functionality is available. Usually, it's allowed by implementing {@link DumbAware} interface.<p></p>
 * <p>
 * "Dumb" mode starts and ends in a {@link consulo.ide.impl.idea.openapi.application.WriteAction}, so if you're inside a {@link ReadAction}
 * on a background thread, it won't suddenly begin in the middle of your operation. But note that whenever you start
 * a top-level read action on a background thread, you should be prepared to anything being changed, including "dumb"
 * mode being suddenly on and off. To avoid executing a read action in "dumb" mode, please use {@link #runReadActionInSmartMode} or
 * {@link NonBlockingReadAction#inSmartMode}.
 * <p>
 * More information about dumb mode could be found here: {@link IndexNotReadyException}
 *
 * @author peter
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class DumbService {
    /**
     * The tracker is advanced each time we enter/exit from dumb mode.
     */
    public abstract ModificationTracker getModificationTracker();

    /**
     * To avoid race conditions use it only in EDT thread or inside read-action. See documentation for this class {@link DumbService}
     *
     * @return whether the IDE is in dumb mode, which means that right now indexes are updated in the background.
     * The IDE offers only limited functionality at such times, e.g., plain text file editing and version control operations.
     */
    public abstract boolean isDumb();

    public static boolean isDumb(@Nonnull Project project) {
        return getInstance(project).isDumb();
    }

    @Nonnull
    public static <T> List<T> getDumbAwareExtensions(@Nonnull Project project, @Nonnull ExtensionPointName<T> extensionPoint) {
        List<T> list = extensionPoint.getExtensionList();
        if (list.isEmpty()) {
            return list;
        }

        DumbService dumbService = getInstance(project);
        return dumbService.filterByDumbAwareness(list);
    }

    @Nonnull
    public static <T> List<T> getDumbAwareExtensions(@Nonnull Project project, @Nonnull ExtensionPoint<T> extensionPoint) {
        List<T> list = extensionPoint.getExtensionList();
        if (list.isEmpty()) {
            return list;
        }

        DumbService dumbService = getInstance(project);
        return dumbService.filterByDumbAwareness(list);
    }

    @Nonnull
    public static <C extends ComponentManager, T> List<T> getDumbAwareExtensions(
        @Nonnull Project project,
        @Nonnull C component,
        @Nonnull ExtensionList<T, C> extensionPoint
    ) {
        List<T> list = extensionPoint.getExtensionList(component);
        if (list.isEmpty()) {
            return list;
        }

        DumbService dumbService = getInstance(project);
        return dumbService.filterByDumbAwareness(list);
    }

    /**
     * Executes the runnable as soon as possible on AWT Event Dispatch when:
     * <ul>
     * <li>project is initialized</li>
     * <li>and there's no dumb mode in progress</li>
     * </ul>
     * This may also happen immediately if these conditions are already met.<p/>
     * Note that it's not guaranteed that the dumb mode won't start again during this runnable execution,
     * it should manage that situation explicitly.
     */
    public abstract void runWhenSmart(@Nonnull Runnable runnable);

    /**
     * Pause the current thread until dumb mode ends and then continue execution.
     * NOTE: there are no guarantees that a new dumb mode won't begin before the next statement.
     * Hence: use with care. Consider using {@link #runWhenSmart(Runnable)} or {@link #runReadActionInSmartMode(Runnable)} instead
     */
    public abstract void waitForSmartMode();

    /**
     * Pause the current thread until dumb mode ends, and then run the read action.
     * Indexes are guaranteed to be available inside that read action, unless this method is already called with read access allowed.
     *
     * @throws ProcessCanceledException if the project is closed during dumb mode
     */
    public <T> T runReadActionInSmartMode(@Nonnull Supplier<T> r) {
        SimpleReference<T> result = SimpleReference.create();
        runReadActionInSmartMode(() -> result.set(r.get()));
        return result.get();
    }

    @Nullable
    public <T> T tryRunReadActionInSmartMode(@Nonnull Supplier<T> task, @Nullable String notification) {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            try {
                return task.get();
            }
            catch (IndexNotReadyException e) {
                if (notification != null) {
                    showDumbModeNotification(notification);
                }
                return null;
            }
        }
        else {
            return runReadActionInSmartMode(task);
        }
    }

    /**
     * Pause the current thread until dumb mode ends, and then run the read action.
     * Indexes are guaranteed to be available inside that read action, unless this method is already called with read access allowed.
     *
     * @throws ProcessCanceledException if the project is closed during dumb mode
     */
    public void runReadActionInSmartMode(@Nonnull Runnable r) {
        if (Application.get().isReadAccessAllowed()) {
            // we can't wait for smart mode to begin (it'd result in a deadlock),
            // so let's just pretend it's already smart and fail with IndexNotReadyException if not
            r.run();
            return;
        }

        while (true) {
            waitForSmartMode();
            boolean success = AccessRule.read(() -> {
                if (getProject().isDisposed()) {
                    throw new ProcessCanceledException();
                }
                if (isDumb()) {
                    return false;
                }
                r.run();
                return true;
            });
            if (success) {
                break;
            }
        }
    }

    /**
     * Pause the current thread until dumb mode ends, and then attempt to execute the runnable.
     * If it fails due to another dumb mode having started, try again until the runnable can complete successfully.
     *
     * @deprecated This method provides no guarantees and should be avoided, please use {@link #runReadActionInSmartMode} instead.
     */
    @Deprecated
    public void repeatUntilPassesInSmartMode(@Nonnull Runnable r) {
        while (true) {
            waitForSmartMode();
            try {
                r.run();
                return;
            }
            catch (IndexNotReadyException ignored) {
            }
        }
    }

    /**
     * Invoke the runnable later on EventDispatchThread AND when IDE isn't in dumb mode.
     * The runnable won't be invoked if the project is disposed during dumb mode.
     */
    public abstract void smartInvokeLater(@Nonnull Runnable runnable);

    /**
     * Invoke the runnable later on EventDispatchThread with the given modality state AND when IDE isn't in dumb mode.
     * The runnable won't be invoked if the project is disposed during dumb mode.
     */
    public abstract void smartInvokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState);

    private static final Function<Project, DumbService> INSTANCE_KEY = ComponentUtil.createLazyInject(DumbService.class);

    public static DumbService getInstance(@Nonnull Project project) {
        return INSTANCE_KEY.apply(project);
    }

    /**
     * @return all the elements of the given array if there's no dumb mode currently,
     * or the dumb-aware ones if {@link #isDumb()} is true.
     * @see #isDumbAware(Object)
     */
    @Nonnull
    public <T> List<T> filterByDumbAwareness(@Nonnull T[] array) {
        return filterByDumbAwareness(Arrays.asList(array));
    }

    /**
     * @return all the elements of the given collection if there's no dumb mode currently,
     * or the dumb-aware ones if {@link #isDumb()} is true.
     * @see #isDumbAware(Object)
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> List<T> filterByDumbAwareness(@Nonnull Collection<? extends T> collection) {
        if (isDumb()) {
            ArrayList<T> result = new ArrayList<>(collection.size());
            for (T element : collection) {
                if (isDumbAware(element)) {
                    result.add(element);
                }
            }
            return result;
        }

        if (collection instanceof List) {
            return (List<T>)collection;
        }

        return new ArrayList<>(collection);
    }

    /**
     * Iterable collection, and skip objects if project is dumb mode, and extension not support dumb mode
     */
    public <T> void forEachDumAwareness(@Nonnull Iterable<? extends T> collection, Consumer<T> consumer) {
        if (isDumb()) {
            for (T object : collection) {
                if (isDumbAware(object)) {
                    consumer.accept(object);
                }
            }
        }
        else {
            collection.forEach(consumer);
        }
    }

    /**
     * Queues a task to be executed in "dumb mode", where access to indexes is forbidden. Tasks are executed sequentially
     * in background unless {@link #completeJustSubmittedTasks()} is called in the same dispatch thread activity.<p/>
     * <p>
     * Tasks can specify custom "equality" policy via their constructor. Calling this method has no effect
     * if an "equal" task is already enqueued (but not yet running).
     */
    public abstract void queueTask(@Nonnull DumbModeTask task);

    /**
     * Cancels the given task. If it's in the queue, it won't be executed. If it's already running,
     * its {@link ProgressIndicator} is canceled, so the next {@link ProgressManager#checkCanceled()} call
     * will throw {@link ProcessCanceledException}.
     */
    public abstract void cancelTask(@Nonnull DumbModeTask task);

    /**
     * Runs the "just submitted" tasks under a modal dialog. "Just submitted" means that tasks were queued for execution
     * earlier within the same Swing event dispatch thread event processing, and there were no other tasks already running
     * at that moment. Otherwise, this method does nothing.<p/>
     * <p>
     * This functionality can be useful in refactorings (invoked in "smart mode"), when after VFS or root changes
     * (which could start "dumb mode") some reference resolve is required (which again requires "smart mode").<p/>
     * <p>
     * Should be invoked on dispatch thread.
     */
    public abstract void completeJustSubmittedTasks();

    /**
     * Replaces given component temporarily with "Not available until indices are built" label during dumb mode.
     *
     * @return Wrapped component.
     */
    public JComponent wrapGently(@Nonnull JComponent dumbUnawareContent, @Nonnull Disposable parentDisposable) {
        throw new AbstractMethodError("AWT/Swing Dependency");
    }

    /**
     * Disables given component temporarily during dumb mode.
     */
    public void makeDumbAware(@Nonnull final JComponent componentToDisable, @Nonnull Disposable parentDisposable) {
        componentToDisable.setEnabled(!isDumb());
        getProject().getMessageBus().connect(parentDisposable).subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                componentToDisable.setEnabled(false);
            }

            @Override
            public void exitDumbMode() {
                componentToDisable.setEnabled(true);
            }
        });
    }

    /**
     * Show a notification when given action is not available during dumb mode.
     */
    public abstract void showDumbModeNotification(@Nonnull LocalizeValue message);

    /**
     * Show a notification when given action is not available during dumb mode.
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void showDumbModeNotification(@Nonnull String message) {
        showDumbModeNotification(LocalizeValue.of(message));
    }

    public abstract Project getProject();

    public static boolean isDumbAware(Object o) {
        return o instanceof PossiblyDumbAware possiblyDumbAware ? possiblyDumbAware.isDumbAware() : o instanceof DumbAware;
    }

    /**
     * Enables or disables alternative resolve strategies for the current thread.<p/>
     * <p>
     * Normally reference resolution uses indexes, and hence is not available in dumb mode. In some cases, alternative ways
     * of performing resolve are available, although much slower. It's impractical to always use these ways because it'll
     * lead to overloaded CPU (especially given there's also indexing in progress). But for some explicit user actions
     * (e.g., explicit Goto Declaration) turning on these slower methods is beneficial.<p/>
     * <p>
     * NOTE: even with alternative resolution enabled, methods like resolve(), findClass() etc may still throw
     * {@link IndexNotReadyException}. So alternative resolve is not a panacea, it might help provide navigation in some cases
     * but not in all.<p/>
     * <p>
     * A typical usage would involve {@code try-finally}, where the alternative resolution is first enabled, then an action is performed,
     * and then alternative resolution is turned off in the {@code finally} block.
     */
    public abstract void setAlternativeResolveEnabled(boolean enabled);

    /**
     * Invokes the given runnable with alternative resolve set to true.
     *
     * @see #setAlternativeResolveEnabled(boolean)
     */
    public void withAlternativeResolveEnabled(@Nonnull Runnable runnable) {
        setAlternativeResolveEnabled(true);
        try {
            runnable.run();
        }
        finally {
            setAlternativeResolveEnabled(false);
        }
    }

    /**
     * Invokes the given computable with alternative resolve set to true.
     *
     * @see #setAlternativeResolveEnabled(boolean)
     */
    public <T, E extends Throwable> T computeWithAlternativeResolveEnabled(@Nonnull ThrowableComputable<T, E> runnable) throws E {
        setAlternativeResolveEnabled(true);
        try {
            return runnable.compute();
        }
        finally {
            setAlternativeResolveEnabled(false);
        }
    }

    /**
     * Invokes the given runnable with alternative resolve set to true.
     *
     * @see #setAlternativeResolveEnabled(boolean)
     */
    public <E extends Throwable> void runWithAlternativeResolveEnabled(@Nonnull ThrowableRunnable<E> runnable) throws E {
        setAlternativeResolveEnabled(true);
        try {
            runnable.run();
        }
        finally {
            setAlternativeResolveEnabled(false);
        }
    }

    /**
     * Invokes the given supplier with alternative resolve set to true, and return value
     *
     * @see #setAlternativeResolveEnabled(boolean)
     */
    public <V, E extends Throwable> V runWithAlternativeResolveEnabled(@Nonnull ThrowableSupplier<V, E> runnable) throws E {
        setAlternativeResolveEnabled(true);
        try {
            return runnable.get();
        }
        finally {
            setAlternativeResolveEnabled(false);
        }
    }

    /**
     * @return whether alternative resolution is enabled for the current thread.
     * @see #setAlternativeResolveEnabled(boolean)
     */
    public abstract boolean isAlternativeResolveEnabled();

    /**
     * @see #completeJustSubmittedTasks()
     * @deprecated Obsolete, does nothing, just executes the passed runnable.
     */
    @Deprecated
    @SuppressWarnings({"unused"})
    public static void allowStartingDumbModeInside(@Nonnull DumbModePermission permission, @Nonnull Runnable runnable) {
        runnable.run();
    }

    /**
     * Runs a heavy activity and suspends indexing (if any) for this time. The user still can manually pause and resume the indexing.
     * In that case, indexing won't be resumed automatically after the activity finishes.
     *
     * @param activityName the text (a noun phrase) to display as a reason for the indexing being paused
     */
    public void suspendIndexingAndRun(@Nonnull LocalizeValue activityName, @Nonnull Runnable activity) {
        try (AccessToken ignore = startHeavyActivityStarted(activityName)) {
            activity.run();
        }
    }

    /**
     * Runs a heavy activity and suspends indexing (if any) for this time. The user still can manually pause and resume the indexing.
     * In that case, indexing won't be resumed automatically after the activity finishes.
     *
     * @param activityName the text (a noun phrase) to display as a reason for the indexing being paused
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void suspendIndexingAndRun(@Nonnull String activityName, @Nonnull Runnable activity) {
        suspendIndexingAndRun(LocalizeValue.of(activityName), activity);
    }

    /**
     * Suspend indexing. The user still can manually pause and resume the indexing. In that case, indexing won't be
     * resumed automatically after the activity finishes.
     *
     * @param activityName the text (a noun phrase) to display as a reason for the indexing being paused
     */
    @Nonnull
    public abstract AccessToken startHeavyActivityStarted(@Nonnull LocalizeValue activityName);

    /**
     * Suspend indexing. The user still can manually pause and resume the indexing. In that case, indexing won't be
     * resumed automatically after the activity finishes.
     *
     * @param activityName the text (a noun phrase) to display as a reason for the indexing being paused
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public AccessToken startHeavyActivityStarted(@Nonnull String activityName) {
        return startHeavyActivityStarted(LocalizeValue.of(activityName));
    }

    /**
     * Checks whether {@link #isDumb()} is true for the current project and if it's currently suspended by user
     * or a {@link #suspendIndexingAndRun} call. This should be called inside read action. The momentary system state is returned:
     * there are no guarantees that the result won't change in the next line of the calling code.
     */
    public abstract boolean isSuspendedDumbMode();
}
