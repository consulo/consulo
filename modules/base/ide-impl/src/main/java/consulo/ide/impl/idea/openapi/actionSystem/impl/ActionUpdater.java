// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.AlwaysPerformingActionGroup;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.logging.Logger;
import consulo.project.internal.DumbInternalUtil;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.XmlActionGroupStub;
import consulo.util.collection.*;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineContextOwner;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineException;
import consulo.util.lang.function.Predicates;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ActionUpdater {
    private static final Logger LOG = Logger.getInstance(ActionUpdater.class);
    private static final Executor ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Action Updater", 2);

    private final PresentationFactory myFactory;
    private final DataContext myDataContext;
    private final String myPlace;
    private final boolean myContextMenuAction;
    private final boolean myToolbarAction;
    private final Project myProject;

    private final boolean myDumbMode;
    private final Map<AnAction, Presentation> myUpdatedPresentations = new ConcurrentHashMap<>();
    private final Map<ActionGroup, List<AnAction>> myGroupChildren = new ConcurrentHashMap<>();
    private final Map<ActionGroup, Boolean> myCanBePerformedCache = new ConcurrentHashMap<>();
    private final UpdateStrategy myRealUpdateStrategy;
    private final ActionManager myActionManager;
    private final UIAccess myUiAccess;

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
        myRealUpdateStrategy = new UpdateStrategy(
            action -> {
                // clone the presentation to avoid partially changing the cached one if update is interrupted
                Presentation presentation = myFactory.getPresentation(action).clone();
                presentation.setEnabledAndVisible(true);
                AnActionEvent event = createActionEvent(action, presentation);
                boolean success = updateAction(action, event);
                return success ? presentation : null;
            },
            group -> group.getChildren(createActionEvent(
                group,
                orDefault(
                    group,
                    myUpdatedPresentations
                        .get(group)
                )
            )),
            group -> myUpdatedPresentations.get(group).isPerformGroup()
        );
    }

    private void applyPresentationChanges() {
        for (Map.Entry<AnAction, Presentation> entry : myUpdatedPresentations.entrySet()) {
            Presentation original = myFactory.getPresentation(entry.getKey());
            Presentation cloned = entry.getValue();

            original.copyFrom(cloned);
        }
    }

    private DataContext getDataContext(AnAction action) {
        return myDataContext;
    }


    private List<AnAction> expandActionGroup(ActionGroup group, boolean hideDisabled, UpdateStrategy strategy) {
        return removeUnnecessarySeparators(doExpandActionGroup(group, hideDisabled, strategy));
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
        CompletableFuture<List<? extends AnAction>> future = new CompletableFuture<>();

        future.whenComplete((anActions, throwable) -> {
            if (throwable != null) {
                indicator.cancel();

                myUiAccess.execute(this::applyPresentationChanges);
            }
        });

        ourExecutor.execute(() -> {
            try {
                ProgressManager.getInstance().runProcess(
                    () -> {
                        List<AnAction> result = expandActionGroup(group, hideDisabled, myRealUpdateStrategy);

                        myUiAccess.execute(() -> {
                            applyPresentationChanges();
                            future.complete(result);
                        });
                    },
                    new SensitiveProgressWrapper(indicator)
                );
            }
            catch (ProcessCanceledException e) {
                future.cancel(false);
            }
            catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private List<AnAction> doExpandActionGroup(ActionGroup group, boolean hideDisabled, UpdateStrategy strategy) {
        if (group instanceof XmlActionGroupStub) {
            throw new IllegalStateException("Trying to expand non-unstubbed group");
        }
        ProgressManager.checkCanceled();

        Presentation presentation = update(group, strategy);
        if (presentation == null || !presentation.isVisible()) { // don't process invisible groups
            return Collections.emptyList();
        }

        List<AnAction> children = getGroupChildren(group, strategy);
        List<AnAction> result = ContainerUtil.concat(children, child -> expandGroupChild(child, hideDisabled, strategy));
        return group.postProcessVisibleChildren(result);
    }

    private List<AnAction> getGroupChildren(ActionGroup group, UpdateStrategy strategy) {
        return myGroupChildren.computeIfAbsent(
            group,
            __ -> {
                AnAction[] children = strategy.getChildren.apply(group);
                int nullIndex = ArrayUtil.indexOf(children, null);
                if (nullIndex < 0) {
                    return Arrays.asList(children);
                }

                LOG.error("action is null: i=" + nullIndex + " group=" + group + " group id=" + myActionManager.getId(group));
                return ContainerUtil.filter(children, Predicates.notNull());
            }
        );
    }

    private List<AnAction> expandGroupChild(AnAction child, boolean hideDisabled, UpdateStrategy strategy) {
        Presentation presentation = update(child, strategy);
        if (presentation == null) {
            return Collections.emptyList();
        }

        if (!presentation.isVisible() || (!presentation.isEnabled() && hideDisabled)) { // don't create invisible items in the menu
            return Collections.emptyList();
        }
        if (child instanceof ActionGroup actionGroup) {
            JBIterable<AnAction> childrenIterable = iterateGroupChildren(actionGroup, strategy);
            if (!presentation.isVisible() || (!presentation.isEnabled() && hideDisabled)) {
                return Collections.emptyList();
            }

            boolean isPopup = actionGroup.isPopup(myPlace);
            boolean hasEnabled = false, hasVisible = false;
            if (hideDisabled || isPopup) {
                for (AnAction action : childrenIterable) {
                    Presentation p = update(action, strategy);
                    if (p == null) {
                        continue;
                    }
                    hasVisible |= p.isVisible();
                    hasEnabled |= p.isEnabled();
                    // stop early if all the required flags are collected
                    if (hasEnabled && hasVisible) {
                        break;
                    }
                    if (hideDisabled && hasEnabled && !isPopup) {
                        break;
                    }
                    if (isPopup && hasVisible && !hideDisabled) {
                        break;
                    }
                }
            }

            if (hideDisabled && !hasEnabled) {
                return Collections.emptyList();
            }
            if (isPopup) {
                boolean canBePerformed = canBePerformed(actionGroup, strategy);
                boolean performOnly = canBePerformed && (actionGroup instanceof AlwaysPerformingActionGroup || !hasVisible);
                presentation.putClientProperty("actionGroup.perform.only", performOnly ? true : null);

                if (!hasVisible && actionGroup.disableIfNoVisibleChildren()) {
                    if (actionGroup.hideIfNoVisibleChildren()) {
                        return Collections.emptyList();
                    }
                    if (!canBePerformed) {
                        presentation.setEnabled(false);
                    }
                }

                if (hideDisabled && !(child instanceof CompactActionGroup)) {
                    return Collections.singletonList(new EmptyAction.DelegatingCompactActionGroup((ActionGroup) child));
                }
                return Collections.singletonList(child);
            }

            return doExpandActionGroup((ActionGroup) child, hideDisabled || actionGroup instanceof CompactActionGroup, strategy);
        }

        return Collections.singletonList(child);
    }


    private JBIterable<AnAction> iterateGroupChildren(ActionGroup group, UpdateStrategy strategy) {
        return JBTreeTraverser.<AnAction>from(o -> {
                if (o == group) {
                    return null;
                }
                if (o instanceof AlwaysVisibleActionGroup) {
                    return null;
                }
                if (myDumbMode && !o.isDumbAware()) {
                    return null;
                }
                if (!(o instanceof ActionGroup oo)) {
                    return null;
                }
                Presentation presentation = update(oo, strategy);
                if (presentation == null || !presentation.isVisible()) {
                    return null;
                }
                if (oo.isPopup(myPlace) || strategy.canBePerformed.test(oo)) {
                    return null;
                }
                return getGroupChildren(oo, strategy);
            })
            .withRoots(getGroupChildren(group, strategy))
            .unique()
            .traverse(TreeTraversal.LEAVES_DFS)
            .filter(o -> !(o instanceof AnSeparator) && !(myDumbMode && !o.isDumbAware()))
            .take(1000);
    }

    boolean canBePerformedCached(ActionGroup group) {
        return !Boolean.FALSE.equals(myCanBePerformedCache.get(group));
    }

    private boolean canBePerformed(ActionGroup group, UpdateStrategy strategy) {
        return myCanBePerformedCache.computeIfAbsent(group, __ -> strategy.canBePerformed.test(group));
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
        return event;
    }

    private boolean hasEnabledChildren(ActionGroup group, UpdateStrategy strategy) {
        return hasChildrenWithState(group, false, true, strategy);
    }

    boolean hasVisibleChildren(ActionGroup group) {
        return hasVisibleChildren(group, myRealUpdateStrategy);
    }

    private boolean hasVisibleChildren(ActionGroup group, UpdateStrategy strategy) {
        return hasChildrenWithState(group, true, false, strategy);
    }

    private boolean hasChildrenWithState(ActionGroup group, boolean checkVisible, boolean checkEnabled, UpdateStrategy strategy) {
        if (group instanceof AlwaysVisibleActionGroup) {
            return true;
        }

        for (AnAction anAction : getGroupChildren(group, strategy)) {
            ProgressManager.checkCanceled();
            if (anAction instanceof AnSeparator) {
                continue;
            }
            if (myDumbMode && !anAction.isDumbAware()) {
                continue;
            }

            Presentation presentation = orDefault(anAction, update(anAction, strategy));
            if (anAction instanceof ActionGroup childGroup) {
                // popup menu must be visible itself
                if (childGroup.isPopup()) {
                    if ((checkVisible && !presentation.isVisible()) || (checkEnabled && !presentation.isEnabled())) {
                        continue;
                    }
                }

                if (hasChildrenWithState(childGroup, checkVisible, checkEnabled, strategy)) {
                    return true;
                }
            }
            else if ((checkVisible && presentation.isVisible()) || (checkEnabled && presentation.isEnabled())) {
                return true;
            }
        }

        return false;
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

    @Nullable
    private Presentation update(AnAction action, UpdateStrategy strategy) {
        Presentation cached = myUpdatedPresentations.get(action);
        if (cached != null) {
            return cached;
        }

        Presentation presentation = strategy.update.apply(action);
        if (presentation != null) {
            myUpdatedPresentations.put(action, presentation);
        }
        return presentation;
    }

    /**
     * Executes the action update using the coroutine-based {@link AnAction#updateAsync(AnActionEvent)} method.
     * <p>
     * On a background thread, builds a coroutine chain that runs the action's update logic.
     *
     * @return true if update succeeded, false if exception was thrown and handled
     */
    private boolean updateAction(AnAction action, AnActionEvent e) {
        if (Application.get().isDisposed()) {
            return false;
        }

        long startTime = System.currentTimeMillis();
        boolean result;
        try {
            result = !performDumbAwareUpdateViaCoroutine(action, e);
        }
        catch (ProcessCanceledException ex) {
            throw ex;
        }
        catch (Throwable exc) {
            handleUpdateException(action, e.getPresentation(), exc);
            return false;
        }
        long endTime = System.currentTimeMillis();
        if (endTime - startTime > 10 && LOG.isDebugEnabled()) {
            LOG.debug("Action " + action + ": updated in " + (endTime - startTime) + " ms");
        }
        return result;
    }

    /**
     * Performs a dumb-aware action update, using the coroutine-based update path for thread routing.
     *
     * @return true if IndexNotReadyException was thrown and handled (dumb mode), false otherwise
     */
    private boolean performDumbAwareUpdateViaCoroutine(AnAction action, AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Boolean wasEnabledBefore = (Boolean) presentation.getClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB);
        if (wasEnabledBefore != null && !myDumbMode) {
            presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, null);
            presentation.setEnabled(wasEnabledBefore);
            presentation.setVisible(true);
        }
        boolean enabledBeforeUpdate = presentation.isEnabled();
        boolean notAllowed = myDumbMode && !action.isDumbAware();

        try {
            executeActionUpdate(action, e);

            presentation.putClientProperty(ActionImplUtil.WOULD_BE_ENABLED_IF_NOT_DUMB_MODE, notAllowed && presentation.isEnabled());
            presentation.putClientProperty(ActionImplUtil.WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE, notAllowed && presentation.isVisible());
        }
        catch (IndexNotReadyException e1) {
            if (notAllowed) {
                return true;
            }
            throw e1;
        }
        finally {
            if (notAllowed) {
                if (wasEnabledBefore == null) {
                    presentation.putClientProperty(ActionImplUtil.WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
                }
                presentation.setEnabled(false);
            }
        }

        return false;
    }

    /**
     * Executes the action update using the coroutine-based {@code action.updateAsync(e)}.
     * <p>
     * Must be called from a background thread. The coroutine runs inside a {@link CoroutineScope#launch}
     * which blocks until completion. A {@link ProgressIndicatorListener} is registered on the current
     * {@link ProgressIndicator} to cancel the coroutine scope when the indicator is cancelled.
     */
    private void executeActionUpdate(AnAction action, AnActionEvent e) {
        LOG.assertTrue(!UIAccess.isUIThread(), "executeActionUpdate must not be called from EDT — CoroutineScope would deadlock");

        Coroutine<?, ?> coroutine = action.updateAsync(e);

        CoroutineContext context = myProject instanceof CoroutineContextOwner owner
            ? owner.coroutineContext()
            : Application.get().coroutineContext();

        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

        try {
            CoroutineScope.launch(context, rScope -> {
                rScope.putCopyableUserData(UIAccess.KEY, myUiAccess);

                if (indicator != null) {
                    indicator.addListener(new ProgressIndicatorListener() {
                        @Override
                        public void canceled() {
                            rScope.cancel();
                        }
                    });
                }
                coroutine.runAsync(rScope, null);
            });
        }
        catch (CoroutineException ex) {
            // Unwrap the original exception from the coroutine scope.
            // CoroutineScope may throw CoroutineScopeException (from catch block)
            // or CoroutineException (from checkThrowErrors when single continuation failed with CoroutineException).
            // Both wrap the original exception as the cause.
            Throwable cause = ex.getCause();
            if (cause instanceof ProcessCanceledException pce) {
                throw pce;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw ex;
        }
    }

    private static class UpdateStrategy {
        final Function<AnAction, Presentation> update;
        final Function<ActionGroup, AnAction[]> getChildren;
        final Predicate<ActionGroup> canBePerformed;

        UpdateStrategy(
            Function<AnAction, Presentation> update,
            Function<ActionGroup, AnAction[]> getChildren,
            Predicate<ActionGroup> canBePerformed
        ) {
            this.update = update;
            this.getChildren = getChildren;
            this.canBePerformed = canBePerformed;
        }
    }
}
