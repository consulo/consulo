// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.impl.internal.action;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.AlwaysPerformingActionGroup;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.DumbInternalUtil;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.ui.ex.internal.XmlActionGroupStub;
import consulo.util.concurrent.coroutine.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActionUpdater {
    private static final Logger LOG = Logger.getInstance(ActionUpdater.class);

    /**
     * A menu or toolbar shows once all its items are updated, so an item updated in the background gets this long: then it is
     * shown with the presentation it had, disabled, and its update is abandoned - it goes on in its own copy of the
     * presentation, which is never applied. A slow update (or a slow group's children) is reported once per class.
     */
    private static final long SLOW_UPDATE_MILLIS = 300;
    private static final Set<Class<?>> ourReportedSlowUpdates = ConcurrentHashMap.newKeySet();

    private final PresentationFactory myFactory;
    private final DataContext myDataContext;
    private final String myPlace;
    private final boolean myContextMenuAction;
    private final boolean myToolbarAction;
    private final Project myProject;

    private final boolean myDumbMode;
    private final Map<AnAction, CompletableFuture<Presentation>> myUpdatedPresentations = new ConcurrentHashMap<>();
    private final Map<ActionGroup, CompletableFuture<List<AnAction>>> myGroupChildren = new ConcurrentHashMap<>();
    private final Map<ActionGroup, Boolean> myCanBePerformedCache = new ConcurrentHashMap<>();
    private final ActionManager myActionManager;
    private final UIAccess myUiAccess;

    private volatile @Nullable ProgressIndicator myExpansionIndicator;

    private final Map<Key<?>, SharedValue> mySharedData = new ConcurrentHashMap<>();

    private record PendingUpdate(long started, CompletableFuture<Presentation> result, @Nullable CoroutineScope scope) {
    }

    private final Map<AnAction, PendingUpdate> myPendingUpdates = new ConcurrentHashMap<>();
    private final AtomicBoolean myDeadlineCheckScheduled = new AtomicBoolean();
    private final Map<AnAction, Presentation> myTimedOutPresentations = new ConcurrentHashMap<>();

    private final ActionUpdateSession mySession = new ActionUpdateSession() {
        @Override
        public CompletableFuture<Presentation> presentation(AnAction action) {
            return updateAsync(action).thenApply(presentation -> orDefault(action, presentation));
        }

        @Override
        public CompletableFuture<List<AnAction>> children(ActionGroup group) {
            return getGroupChildrenAsync(group);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T sharedData(Key<T> key, Supplier<? extends T> provider) {
            // the provider runs outside the map: it may take a while, and ask for other shared data
            return (T) mySharedData.computeIfAbsent(key, __ -> new SharedValue()).get(provider);
        }
    };

    private static final class SharedValue {
        private static final Object NOT_COMPUTED = new Object();

        private volatile @Nullable Object myValue = NOT_COMPUTED;

        @Nullable Object get(Supplier<?> provider) {
            Object value = myValue;
            if (value == NOT_COMPUTED) {
                synchronized (this) {
                    value = myValue;
                    if (value == NOT_COMPUTED) {
                        myValue = value = provider.get();
                    }
                }
            }
            return value;
        }
    }

    public ActionUpdateSession asUpdateSession() {
        return mySession;
    }

    public ActionUpdater(
        ActionManager actionManager,
        PresentationFactory presentationFactory,
        DataContext dataContext,
        String place,
        boolean isContextMenuAction,
        boolean isToolbarAction,
        UIAccess uiAccess
    ) {
        myUiAccess = uiAccess;
        myProject = dataContext.getData(Project.KEY);
        myDumbMode = DumbInternalUtil.isDumbMode(myProject);
        myActionManager = actionManager;
        myFactory = presentationFactory;
        myDataContext = dataContext;
        myPlace = place;
        myContextMenuAction = isContextMenuAction;
        myToolbarAction = isToolbarAction;
    }

    private void applyPresentationChanges() {
        for (Map.Entry<AnAction, CompletableFuture<Presentation>> entry : myUpdatedPresentations.entrySet()) {
            CompletableFuture<Presentation> future = entry.getValue();
            Presentation cloned = future.getNow(null);
            if (cloned == null) {
                continue;
            }
            Presentation original = myFactory.getPresentation(entry.getKey());
            original.copyFrom(cloned);
        }
    }

    private @Nullable Presentation cachedPresentation(AnAction action) {
        CompletableFuture<Presentation> future = myUpdatedPresentations.get(action);
        return future != null ? future.getNow(null) : null;
    }

    private DataContext getDataContext(AnAction action) {
        return myDataContext;
    }


    public CompletableFuture<List<? extends AnAction>> expandActionGroupAsync(
        ActionGroup group,
        boolean hideDisabled
    ) {
        return expandActionGroupAsync(group, hideDisabled, new EmptyProgressIndicator());
    }

    public CompletableFuture<List<? extends AnAction>> expandActionGroupAsync(
        ActionGroup group,
        boolean hideDisabled,
        ProgressIndicator indicator
    ) {
        myExpansionIndicator = indicator;

        CompletableFuture<List<? extends AnAction>> result = new CompletableFuture<>();

        doExpandActionGroupAsync(group, hideDisabled)
            .thenApply(ActionUpdater::removeUnnecessarySeparators)
            .thenApplyAsync(list -> {
                // a newer expansion may have been requested while this one was finishing:
                // never apply stale presentations over the fresher ones
                checkCanceled();
                applyPresentationChanges();
                return (List<? extends AnAction>) list;
            }, myUiAccess::execute)
            .whenComplete((list, throwable) -> {
                if (throwable == null) {
                    result.complete(list);
                    return;
                }

                indicator.cancel();

                Throwable cause = unwrapCoroutineException(throwable);
                if (cause instanceof ProcessCanceledException || cause instanceof CancellationException) {
                    result.cancel(false);
                }
                else {
                    myUiAccess.execute(this::applyPresentationChanges);
                    result.completeExceptionally(throwable);
                }
            });

        return result;
    }

    private CompletableFuture<List<AnAction>> doExpandActionGroupAsync(ActionGroup group, boolean hideDisabled) {
        if (group instanceof XmlActionGroupStub) {
            throw new IllegalStateException("Trying to expand non-unstubbed group");
        }
        checkCanceled();

        return updateAsync(group).thenCompose(presentation -> {
            if (presentation == null || !presentation.isVisible()) { // don't process invisible groups
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            return getGroupChildrenAsync(group).thenCompose(children -> expandChildren(children, hideDisabled)
                .thenApply(group::postProcessVisibleChildren)
                .thenCompose(this::updateInlineActions));
        });
    }

    private CompletableFuture<List<AnAction>> updateInlineActions(List<AnAction> actions) {
        List<CompletableFuture<Presentation>> updates = new ArrayList<>();
        for (AnAction action : actions) {
            Presentation presentation = cachedPresentation(action);
            List<AnAction> inlineActions = presentation == null ? null : presentation.getClientProperty(ActionUtil.INLINE_ACTIONS);
            if (inlineActions == null) {
                continue;
            }

            for (AnAction inlineAction : inlineActions) {
                updates.add(updateAsync(inlineAction));
            }
        }

        if (updates.isEmpty()) {
            return CompletableFuture.completedFuture(actions);
        }

        return CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new)).thenApply(__ -> actions);
    }

    private CompletableFuture<List<AnAction>> expandChildren(List<AnAction> children, boolean hideDisabled) {
        checkCanceled();
        List<CompletableFuture<List<AnAction>>> futures = new ArrayList<>(children.size());
        for (AnAction child : children) {
            futures.add(expandGroupChild(child, hideDisabled));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(__ -> {
            List<AnAction> result = new ArrayList<>();
            for (CompletableFuture<List<AnAction>> future : futures) {
                result.addAll(future.join());
            }
            return result;
        });
    }

    private CompletableFuture<Presentation> updateAsync(AnAction action) {
        return myUpdatedPresentations.computeIfAbsent(action, this::computeUpdatedPresentation);
    }

    private CompletableFuture<Presentation> computeUpdatedPresentation(AnAction action) {
        checkCanceled();
        Presentation presentation = myFactory.getPresentation(action).clone();
        presentation.setEnabledAndVisible(true);
        AnActionEvent event = createActionEvent(action, presentation);
        long started = System.nanoTime();
        SimpleReference<CoroutineScope> scope = SimpleReference.create();
        CompletableFuture<Presentation> updated = updateActionAsync(action, event, scope::set)
            .thenApply(success -> success ? presentation : null);
        // only a menu or toolbar being filled has a deadline - an update before performing waits for its action. The menu needs
        // the UI thread too, so a deadline would not let it show sooner than an update on the UI thread
        if (myExpansionIndicator != null && !updated.isDone() && !ActionUpdateInvoker.isUpdatedOnUIThread(action)) {
            watchDeadline(action, new PendingUpdate(started, updated, scope.get()));
        }
        return updated;
    }

    private void watchDeadline(AnAction action, PendingUpdate pending) {
        myPendingUpdates.put(action, pending);
        pending.result().whenComplete((presentation, throwable) -> myPendingUpdates.remove(action, pending));
        scheduleDeadlineCheck(SLOW_UPDATE_MILLIS);
    }

    /**
     * One check per session, not a timer per item: it runs again only while some update is still pending.
     */
    private void scheduleDeadlineCheck(long delayMillis) {
        if (myDeadlineCheckScheduled.compareAndSet(false, true)) {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(this::checkDeadlines, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void checkDeadlines() {
        ProgressIndicator indicator = myExpansionIndicator;
        if (indicator != null && indicator.isCanceled()) {
            myPendingUpdates.clear();
            myDeadlineCheckScheduled.set(false);
            return;
        }

        long now = System.nanoTime();
        long deadline = TimeUnit.MILLISECONDS.toNanos(SLOW_UPDATE_MILLIS);
        long nextCheck = deadline;
        for (Map.Entry<AnAction, PendingUpdate> entry : myPendingUpdates.entrySet()) {
            long left = entry.getValue().started() + deadline - now;
            if (left <= 0) {
                timeOut(entry.getKey(), entry.getValue());
            }
            else {
                nextCheck = Math.min(nextCheck, left);
            }
        }

        myDeadlineCheckScheduled.set(false);
        if (!myPendingUpdates.isEmpty()) {
            scheduleDeadlineCheck(TimeUnit.NANOSECONDS.toMillis(nextCheck) + 1);
        }
    }

    private void timeOut(AnAction action, PendingUpdate pending) {
        Presentation stale = myFactory.getPresentation(action).clone();
        stale.setEnabled(false);
        myTimedOutPresentations.put(action, stale);
        if (!pending.result().complete(stale)) {
            myTimedOutPresentations.remove(action, stale);
            return;
        }

        CoroutineScope scope = pending.scope();
        if (scope != null) {
            scope.cancel();
        }
        if (ourReportedSlowUpdates.add(action.getClass())) {
            LOG.warn("Update of " + describe(action) + " in " + myPlace + " did not finish in " + SLOW_UPDATE_MILLIS +
                " ms - shown disabled. An update must not wait for anything (locks, model builds)");
        }
    }

    private boolean isTimedOut(AnAction action, @Nullable Presentation presentation) {
        return presentation != null && myTimedOutPresentations.get(action) == presentation;
    }

    private CompletableFuture<List<AnAction>> getGroupChildrenAsync(ActionGroup group) {
        return myGroupChildren.computeIfAbsent(group, _ -> computeGroupChildren(group));
    }

    private CompletableFuture<List<AnAction>> computeGroupChildren(ActionGroup group) {
        checkCanceled();
        AnActionEvent event = createActionEvent(group, orDefault(group, cachedPresentation(group)));
        long started = System.currentTimeMillis();
        return launchCoroutineAsync(group.getChildrenAsync(event), null).thenApply(children -> {
            long elapsed = System.currentTimeMillis() - started;
            if (elapsed >= SLOW_UPDATE_MILLIS && ourReportedSlowUpdates.add(group.getClass())) {
                LOG.warn("Slow children of " + describe(group) + " in " + myPlace + ": " + elapsed +
                    " ms - the menu or toolbar waits for them. Children must not wait for anything (locks, model builds)");
            }
            return children == null ? List.of() : children;
        });
    }

    private CompletableFuture<List<AnAction>> expandGroupChild(AnAction child, boolean hideDisabled) {
        return updateAsync(child).thenCompose(presentation -> {
            if (presentation == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            if (!presentation.isVisible() || (!presentation.isEnabled() && hideDisabled)) { // don't create invisible items in the menu
                return CompletableFuture.completedFuture(Collections.emptyList());
            }
            if (!(child instanceof ActionGroup actionGroup)) {
                return CompletableFuture.completedFuture(Collections.singletonList(child));
            }

            boolean isPopup = actionGroup.isPopup(myPlace) || presentation.isPopupGroup();
            if (isPopup && isTimedOut(child, presentation)) {
                // shown as it was, disabled - a submenu whose update did not finish in time is not asked for its children either
                return CompletableFuture.completedFuture(Collections.singletonList(child));
            }
            return collectChildrenFlags(actionGroup, hideDisabled, isPopup).thenCompose(flags -> {
                boolean hasEnabled = flags.hasEnabled();
                boolean hasVisible = flags.hasVisible();

                if (hideDisabled && !hasEnabled) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }
                if (isPopup) {
                    boolean canBePerformed = canBePerformed(actionGroup);
                    boolean performOnly = canBePerformed && (actionGroup instanceof AlwaysPerformingActionGroup || !hasVisible);
                    presentation.putClientProperty("actionGroup.perform.only", performOnly ? true : null);

                    if (!hasVisible && actionGroup.disableIfNoVisibleChildren()) {
                        if (actionGroup.hideIfNoVisibleChildren()) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                        if (!canBePerformed) {
                            presentation.setEnabled(false);
                        }
                    }

                    if (hideDisabled && !(child instanceof CompactActionGroup)) {
                        return CompletableFuture.completedFuture(List.of(new EmptyAction.DelegatingCompactActionGroup((ActionGroup) child)));
                    }
                    return CompletableFuture.completedFuture(Collections.singletonList(child));
                }

                return doExpandActionGroupAsync((ActionGroup) child, hideDisabled || actionGroup instanceof CompactActionGroup);
            });
        });
    }

    private record ChildrenFlags(boolean hasEnabled, boolean hasVisible) {
    }

    private CompletableFuture<ChildrenFlags> collectChildrenFlags(ActionGroup actionGroup, boolean hideDisabled, boolean isPopup) {
        if (!(hideDisabled || isPopup)) {
            return CompletableFuture.completedFuture(new ChildrenFlags(false, false));
        }
        return iterateGroupChildren(actionGroup)
            .thenCompose(leaves -> foldChildrenFlags(leaves, 0, false, false, hideDisabled, isPopup));
    }

    private CompletableFuture<ChildrenFlags> foldChildrenFlags(
        List<AnAction> leaves, int index, boolean hasEnabled, boolean hasVisible, boolean hideDisabled, boolean isPopup
    ) {
        if (hasEnabled && hasVisible
            || hideDisabled && hasEnabled && !isPopup
            || isPopup && hasVisible && !hideDisabled
            || index >= leaves.size()) {
            return CompletableFuture.completedFuture(new ChildrenFlags(hasEnabled, hasVisible));
        }
        checkCanceled();
        return updateAsync(leaves.get(index)).thenCompose(p -> {
            boolean enabled = hasEnabled;
            boolean visible = hasVisible;
            if (p != null) {
                visible |= p.isVisible();
                enabled |= p.isEnabled();
            }
            return foldChildrenFlags(leaves, index + 1, enabled, visible, hideDisabled, isPopup);
        });
    }

    private CompletableFuture<List<AnAction>> iterateGroupChildren(ActionGroup group) {
        List<AnAction> result = new ArrayList<>();
        Set<AnAction> visited = new HashSet<>();
        return getGroupChildrenAsync(group)
            .thenCompose(roots -> traverseLeaves(roots, 0, group, result, visited))
            .thenApply(__ -> result);
    }

    private CompletableFuture<Void> traverseLeaves(
        List<AnAction> nodes, int index, ActionGroup root, List<AnAction> out, Set<AnAction> visited
    ) {
        if (index >= nodes.size() || out.size() >= 1000) {
            return CompletableFuture.completedFuture(null);
        }
        checkCanceled();
        AnAction o = nodes.get(index);
        if (!visited.add(o)) {
            return traverseLeaves(nodes, index + 1, root, out, visited);
        }
        return descendableChildren(o, root).thenCompose(children -> {
            if (children != null) {
                return traverseLeaves(children, 0, root, out, visited)
                    .thenCompose(__ -> traverseLeaves(nodes, index + 1, root, out, visited));
            }
            if (!(o instanceof AnSeparator) && !(myDumbMode && !o.isDumbAware())) {
                out.add(o);
            }
            return traverseLeaves(nodes, index + 1, root, out, visited);
        });
    }

    private CompletableFuture<List<AnAction>> descendableChildren(AnAction o, ActionGroup root) {
        if (o == root || o instanceof AlwaysVisibleActionGroup || (myDumbMode && !o.isDumbAware()) || !(o instanceof ActionGroup oo)) {
            return CompletableFuture.completedFuture(null);
        }
        return updateAsync(oo).thenCompose(presentation -> {
            if (presentation == null || !presentation.isVisible()
                || oo.isPopup(myPlace) || presentation.isPopupGroup() || computeCanBePerformed(oo)) {
                return CompletableFuture.completedFuture(null);
            }
            return getGroupChildrenAsync(oo);
        });
    }

    private void checkCanceled() {
        ProgressIndicator indicator = myExpansionIndicator;
        if (indicator != null && indicator.isCanceled()) {
            throw new ProcessCanceledException();
        }
        ProgressManager.checkCanceled();
    }

    private boolean canBePerformed(ActionGroup group) {
        return myCanBePerformedCache.computeIfAbsent(group, this::computeCanBePerformed);
    }

    private boolean computeCanBePerformed(ActionGroup group) {
        Presentation p = cachedPresentation(group);
        return p != null && p.isPerformGroup();
    }

    private Presentation orDefault(AnAction action, Presentation presentation) {
        return presentation != null ? presentation : myFactory.getPresentation(action);
    }

    private static List<AnAction> removeUnnecessarySeparators(List<? extends AnAction> visible) {
        List<AnAction> result = new ArrayList<>();
        for (AnAction child : visible) {
            if (child instanceof AnSeparator separator) {
                if (separator.getTextValue().isNotEmpty()
                    || (!result.isEmpty() && !(result.get(result.size() - 1) instanceof AnSeparator))) {
                    result.add(child);
                }
            }
            else {
                result.add(child);
            }
        }
        return result;
    }

    private AnActionEvent createActionEvent(AnAction action, Presentation presentation) {
        AnActionEvent event = new AnActionEvent(
            null,
            getDataContext(action),
            myPlace,
            presentation,
            myActionManager,
            0,
            myContextMenuAction,
            myToolbarAction
        );
        event.setInjectedContext(action.isInInjectedContext());
        event.setUpdateSession(mySession);
        return event;
    }

    private void handleUpdateException(AnAction action, Presentation presentation, Throwable exc) {
        String id = myActionManager.getId(action);
        if (id != null) {
            LOG.error("update failed for AnAction(" + action.getClass().getName() + ") with ID=" + id, exc);
        }
        else {
            LOG.error("update failed for ActionGroup: " + action + "[" + presentation.getText() + "]", exc);
        }
    }

    private CompletableFuture<Boolean> updateActionAsync(AnAction action, AnActionEvent e, @Nullable Consumer<CoroutineScope> onLaunch) {
        if (Application.get().isDisposed()) {
            return CompletableFuture.completedFuture(false);
        }

        Presentation presentation = e.getPresentation();
        Boolean wasEnabledBefore = (Boolean) presentation.getClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB);
        if (wasEnabledBefore != null && !myDumbMode) {
            presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, null);
            presentation.setEnabled(wasEnabledBefore);
            presentation.setVisible(true);
        }
        boolean enabledBeforeUpdate = presentation.isEnabled();
        boolean notAllowed = myDumbMode && !action.isDumbAware();
        long startTime = System.currentTimeMillis();

        Coroutine<?, ?> coroutine = ActionUpdateInvoker.createUpdateCoroutine(action, e);
        CompletableFuture<?> updateFuture = coroutine == null
            ? CompletableFuture.completedFuture(null)
            : launchCoroutineAsync(coroutine, onLaunch);

        return updateFuture.handle((ignored, throwable) -> {
            reportSlowUpdate(action, System.currentTimeMillis() - startTime);
            try {
                if (throwable != null) {
                    Throwable cause = unwrapCoroutineException(throwable);
                    if (cause instanceof ProcessCanceledException pce) {
                        throw pce;
                    }
                    if (cause instanceof CancellationException) {
                        // abandoned after its deadline, or the whole expansion was cancelled
                        return false;
                    }
                    if (cause instanceof IndexNotReadyException) {
                        if (!notAllowed) {
                            handleUpdateException(action, presentation, cause);
                        }
                        return false;
                    }
                    handleUpdateException(action, presentation, cause);
                    return false;
                }

                presentation.putClientProperty(ActionImplUtil.WOULD_BE_ENABLED_IF_NOT_DUMB_MODE, notAllowed && presentation.isEnabled());
                presentation.putClientProperty(ActionImplUtil.WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE, notAllowed && presentation.isVisible());

                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > 10 && LOG.isDebugEnabled()) {
                    LOG.debug("Action " + action + ": updated in " + elapsed + " ms");
                }
                return true;
            }
            finally {
                if (notAllowed) {
                    if (wasEnabledBefore == null) {
                        presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
                    }
                    presentation.setEnabled(false);
                }
            }
        });
    }

    private void reportSlowUpdate(AnAction action, long elapsed) {
        if (elapsed < SLOW_UPDATE_MILLIS || !ourReportedSlowUpdates.add(action.getClass())) {
            return;
        }
        LOG.warn("Slow update of " + describe(action) + " in " + myPlace + ": " + elapsed +
            " ms - the menu or toolbar waits for it. An update must not wait for anything (locks, model builds)");
    }

    private String describe(AnAction action) {
        String id = myActionManager.getId(action);
        return action.getClass().getName() + (id == null ? "" : " (id=" + id + ")");
    }

    private <T> CompletableFuture<T> launchCoroutineAsync(Coroutine<?, T> coroutine, @Nullable Consumer<CoroutineScope> onLaunch) {
        ProgressIndicator expansionIndicator = myExpansionIndicator;
        if (expansionIndicator != null && expansionIndicator.isCanceled()) {
            return CompletableFuture.failedFuture(new ProcessCanceledException());
        }

        CoroutineContext context = myProject instanceof CoroutineContextOwner owner
            ? owner.coroutineContext()
            : Application.get().coroutineContext();

        CoroutineScope scope = CoroutineScope.of(context);
        scope.putCopyableUserData(UIAccess.KEY, myUiAccess);

        ProgressIndicator indicator = myExpansionIndicator;
        if (indicator != null) {
            indicator.addListener(new ProgressIndicatorListener() {
                @Override
                public void canceled() {
                    scope.cancel();
                }
            });
        }

        if (onLaunch != null) {
            onLaunch.accept(scope);
        }
        return coroutine.runAsync(scope, null).toFuture();
    }

    private static Throwable unwrapCoroutineException(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof CoroutineException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
