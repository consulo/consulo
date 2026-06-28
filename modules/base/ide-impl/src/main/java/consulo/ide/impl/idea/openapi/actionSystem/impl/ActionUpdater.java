// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.AlwaysPerformingActionGroup;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.internal.DumbInternalUtil;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.XmlActionGroupStub;
import consulo.util.concurrent.coroutine.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public class ActionUpdater {
    private static final Logger LOG = Logger.getInstance(ActionUpdater.class);

    private final PresentationFactory myFactory;
    private final DataContext myDataContext;
    private final String myPlace;
    private final boolean myContextMenuAction;
    private final boolean myToolbarAction;
    private final Project myProject;

    private final boolean myDumbMode;
    // caches store futures so concurrent expansion shares a single in-flight computation per action/group
    private final Map<AnAction, CompletableFuture<Presentation>> myUpdatedPresentations = new ConcurrentHashMap<>();
    private final Map<ActionGroup, CompletableFuture<List<AnAction>>> myGroupChildren = new ConcurrentHashMap<>();
    private final Map<ActionGroup, Boolean> myCanBePerformedCache = new ConcurrentHashMap<>();
    private final ActionManager myActionManager;
    private final UIAccess myUiAccess;

    // the progress indicator of the currently running async expansion; used to wire coroutine cancellation
    private volatile @Nullable ProgressIndicator myExpansionIndicator;

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
            // expansion has completed when this runs, so the futures are resolved
            Presentation cloned = entry.getValue().getNow(null);
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
            // apply presentation changes and publish the result on the UI thread
            .thenApplyAsync(list -> {
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
                    // cancellation is normal control flow, not an error: signal it as a cancelled future
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
                .thenApply(group::postProcessVisibleChildren));
        });
    }

    /**
     * Expands all children concurrently and concatenates the results preserving the original order.
     */
    private CompletableFuture<List<AnAction>> expandChildren(List<AnAction> children, boolean hideDisabled) {
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

    /**
     * Returns the (cached) updated presentation for an action, computing it asynchronously on first request.
     * The cache stores the future itself so concurrent requests share a single in-flight update.
     */
    private CompletableFuture<Presentation> updateAsync(AnAction action) {
        return myUpdatedPresentations.computeIfAbsent(action, this::computeUpdatedPresentation);
    }

    private CompletableFuture<Presentation> computeUpdatedPresentation(AnAction action) {
        // clone the presentation to avoid partially changing the cached one if update is interrupted
        Presentation presentation = myFactory.getPresentation(action).clone();
        presentation.setEnabledAndVisible(true);
        AnActionEvent event = createActionEvent(action, presentation);
        return updateActionAsync(action, event).thenApply(success -> success ? presentation : null);
    }

    private CompletableFuture<List<AnAction>> getGroupChildrenAsync(ActionGroup group) {
        return myGroupChildren.computeIfAbsent(group, _ -> computeGroupChildren(group));
    }

    private CompletableFuture<List<AnAction>> computeGroupChildren(ActionGroup group) {
        // getChildren is always requested after the group's own update, so its presentation is cached
        AnActionEvent event = createActionEvent(group, orDefault(group, cachedPresentation(group)));
        return launchCoroutineAsync(group.getChildrenAsync(event));
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

            boolean isPopup = actionGroup.isPopup(myPlace);
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

    /**
     * Asynchronously computes whether a group has enabled/visible descendants, mirroring the early-exit loop
     * of the former synchronous implementation.
     */
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
        // stop early if all the required flags are collected (same conditions as the former synchronous loop)
        if (hasEnabled && hasVisible
            || hideDisabled && hasEnabled && !isPopup
            || isPopup && hasVisible && !hideDisabled
            || index >= leaves.size()) {
            return CompletableFuture.completedFuture(new ChildrenFlags(hasEnabled, hasVisible));
        }
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

    /**
     * Asynchronous replacement of the former {@code JBTreeTraverser}-based leaves traversal.
     * Collects up to 1000 unique non-separator leaf actions in DFS order, descending into visible,
     * non-popup, non-performable sub-groups.
     */
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
        AnAction o = nodes.get(index);
        if (!visited.add(o)) { // unique()
            return traverseLeaves(nodes, index + 1, root, out, visited);
        }
        return descendableChildren(o, root).thenCompose(children -> {
            if (children != null) {
                // internal node: descend first (DFS), then continue with the remaining siblings
                return traverseLeaves(children, 0, root, out, visited)
                    .thenCompose(__ -> traverseLeaves(nodes, index + 1, root, out, visited));
            }
            // leaf: keep it unless it is a separator or a non-dumb-aware action in dumb mode
            if (!(o instanceof AnSeparator) && !(myDumbMode && !o.isDumbAware())) {
                out.add(o);
            }
            return traverseLeaves(nodes, index + 1, root, out, visited);
        });
    }

    /**
     * Returns the children to descend into if the node is a traversable sub-group, or {@code null} if it is a leaf.
     */
    private CompletableFuture<List<AnAction>> descendableChildren(AnAction o, ActionGroup root) {
        if (o == root || o instanceof AlwaysVisibleActionGroup || (myDumbMode && !o.isDumbAware()) || !(o instanceof ActionGroup oo)) {
            return CompletableFuture.completedFuture(null);
        }
        return updateAsync(oo).thenCompose(presentation -> {
            if (presentation == null || !presentation.isVisible()
                || oo.isPopup(myPlace) || computeCanBePerformed(oo)) {
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

    /**
     * Runs the action's coroutine-based {@link AnAction#updateAsync(AnActionEvent)} non-blockingly and applies the
     * dumb-aware bookkeeping around it. The returned future yields {@code true} if the action should be kept.
     */
    private CompletableFuture<Boolean> updateActionAsync(AnAction action, AnActionEvent e) {
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

        return launchCoroutineAsync(action.updateAsync(e)).handle((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    Throwable cause = unwrapCoroutineException(throwable);
                    if (cause instanceof ProcessCanceledException pce) {
                        throw pce;
                    }
                    if (cause instanceof IndexNotReadyException) {
                        // in dumb mode this is expected for non-dumb-aware actions; otherwise log it
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

    /**
     * Launches a coroutine non-blockingly in a fresh {@link CoroutineScope} and bridges it to a {@link CompletableFuture}.
     * The current expansion {@link ProgressIndicator} (if any) is wired to cancel the scope.
     */
    private <T> CompletableFuture<T> launchCoroutineAsync(Coroutine<?, T> coroutine) {
        CoroutineContext context = myProject instanceof CoroutineContextOwner owner
            ? owner.coroutineContext()
            : Application.get().coroutineContext();

        CoroutineScope scope = new CoroutineScope(context);
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

        return coroutine.runAsync(scope, null).toFuture();
    }

    /**
     * Unwraps {@link CompletionException} and {@link CoroutineException} layers to expose the original failure cause.
     */
    private static Throwable unwrapCoroutineException(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof CoroutineException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
