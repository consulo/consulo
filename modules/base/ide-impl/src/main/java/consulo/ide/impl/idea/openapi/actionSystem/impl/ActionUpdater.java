// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.impl.internal.progress.SensitiveProgressWrapper;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.actionSystem.AlwaysPerformingActionGroup;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.XmlActionGroupStub;
import consulo.util.collection.*;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Promise;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Conditions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.PaintEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ActionUpdater {
  private static final Logger LOG = Logger.getInstance(ActionUpdater.class);
  private static final Executor ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Action Updater", 2);

  private final boolean myModalContext;
  private final PresentationFactory myFactory;
  private final DataContext myDataContext;
  private final String myPlace;
  private final boolean myContextMenuAction;
  private final boolean myToolbarAction;
  private final Project myProject;

  private final Map<AnAction, Presentation> myUpdatedPresentations = new ConcurrentHashMap<>();
  private final Map<ActionGroup, List<AnAction>> myGroupChildren = new ConcurrentHashMap<>();
  private final Map<ActionGroup, Boolean> myCanBePerformedCache = new ConcurrentHashMap<>();
  private final UpdateStrategy myRealUpdateStrategy;
  private final UpdateStrategy myCheapStrategy;
  private final ActionManager myActionManager;

  private boolean myAllowPartialExpand = true;

  public ActionUpdater(ActionManager actionManager,
                       boolean isInModalContext,
                       PresentationFactory presentationFactory,
                       DataContext dataContext,
                       String place,
                       boolean isContextMenuAction,
                       boolean isToolbarAction) {
    myProject = dataContext.getData(Project.KEY);
    myActionManager = actionManager;
    myModalContext = isInModalContext;
    myFactory = presentationFactory;
    myDataContext = dataContext;
    myPlace = place;
    myContextMenuAction = isContextMenuAction;
    myToolbarAction = isToolbarAction;
    myRealUpdateStrategy = new UpdateStrategy(action -> {
      // clone the presentation to avoid partially changing the cached one if update is interrupted
      Presentation presentation = ActionUpdateEdtExecutor.computeOnEdt(() -> myFactory.getPresentation(action).clone());
      presentation.setEnabledAndVisible(true);
      Supplier<Boolean> doUpdate = () -> doUpdate(myModalContext, action, createActionEvent(action, presentation));
      boolean success = callAction(action, "update", doUpdate);
      return success ? presentation : null;
    },
                                              group -> callAction(group,
                                                                  "getChildren",
                                                                  () -> group.getChildren(createActionEvent(group,
                                                                                                            orDefault(group,
                                                                                                                      myUpdatedPresentations
                                                                                                                        .get(group))))),
                                              group -> callAction(group,
                                                                  "canBePerformed",
                                                                  () -> group.canBePerformed(getDataContext(group))));
    myCheapStrategy = new UpdateStrategy(myFactory::getPresentation, group -> group.getChildren(null), group -> true);
  }

  private void applyPresentationChanges() {
    for (Map.Entry<AnAction, Presentation> entry : myUpdatedPresentations.entrySet()) {
      Presentation original = myFactory.getPresentation(entry.getKey());
      Presentation cloned = entry.getValue();
      original.copyFrom(cloned);
      reflectSubsequentChangesInOriginalPresentation(original, cloned);
    }
  }

  private DataContext getDataContext(@Nonnull AnAction action) {
    return myDataContext;
  }

  // some actions remember the presentation passed to "update" and modify it later, in hope that menu will change accordingly
  private static void reflectSubsequentChangesInOriginalPresentation(Presentation original, Presentation cloned) {
    cloned.addPropertyChangeListener(e -> {
      if (SwingUtilities.isEventDispatchThread()) {
        original.copyFrom(cloned);
      }
    });
  }

  private static <T> T callAction(AnAction action, String operation, Supplier<T> call) {
    if (action instanceof UpdateInBackground || ApplicationManager.getApplication().isDispatchThread()) return call.get();

    ProgressIndicator progress = Objects.requireNonNull(ProgressManager.getInstance().getProgressIndicator());

    return ActionUpdateEdtExecutor.computeOnEdt(() -> {
      long start = System.currentTimeMillis();
      try {
        return ProgressManager.getInstance().runProcess(call, ProgressWrapper.wrap(progress));
      }
      finally {
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 100) {
          LOG.warn("Slow (" + elapsed + "ms) '" + operation + "' on action " + action + " of " + action.getClass() + ". Consider speeding it up and/or implementing UpdateInBackground.");
        }

      }
    });
  }

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   */
  public List<AnAction> expandActionGroup(ActionGroup group, boolean hideDisabled) {
    try {
      return expandActionGroup(group, hideDisabled, myRealUpdateStrategy);
    }
    finally {
      applyPresentationChanges();
    }
  }

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   * don't check progress.isCanceled (to obtain full list of actions)
   */
  public List<AnAction> expandActionGroupFull(ActionGroup group, boolean hideDisabled) {
    try {
      myAllowPartialExpand = false;
      return expandActionGroup(group, hideDisabled, myRealUpdateStrategy);
    }
    finally {
      myAllowPartialExpand = true;
      applyPresentationChanges();
    }
  }

  private List<AnAction> expandActionGroup(ActionGroup group, boolean hideDisabled, UpdateStrategy strategy) {
    return removeUnnecessarySeparators(doExpandActionGroup(group, hideDisabled, strategy));
  }

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   */
  @Nonnull
  public List<AnAction> expandActionGroupWithTimeout(ActionGroup group, boolean hideDisabled) {
    return expandActionGroupWithTimeout(group, hideDisabled, Registry.intValue("actionSystem.update.timeout.ms"));
  }

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   */
  @Nonnull
  public List<AnAction> expandActionGroupWithTimeout(ActionGroup group, boolean hideDisabled, int timeoutMs) {
    List<AnAction> result = ProgressIndicatorUtils.withTimeout(timeoutMs, () -> expandActionGroup(group, hideDisabled));
    try {
      return result != null ? result : expandActionGroup(group, hideDisabled, myCheapStrategy);
    }
    finally {
      applyPresentationChanges();
    }
  }

  public CancellablePromise<List<AnAction>> expandActionGroupAsync(ActionGroup group, boolean hideDisabled) {
    AsyncPromise<List<AnAction>> promise = new AsyncPromise<>();
    ProgressIndicator indicator = new EmptyProgressIndicator();
    promise.onError(__ -> {
      indicator.cancel();
      ActionUpdateEdtExecutor.computeOnEdt(() -> {
        applyPresentationChanges();
        return null;
      });
    });

    cancelAndRestartOnUserActivity(promise, indicator);

    ourExecutor.execute(() -> {
      while (promise.getState() == Promise.State.PENDING) {
        try {
          boolean success = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
            List<AnAction> result = expandActionGroup(group, hideDisabled, myRealUpdateStrategy);
            ActionUpdateEdtExecutor.computeOnEdt(() -> {
              applyPresentationChanges();
              promise.setResult(result);
              return null;
            });
          }, new SensitiveProgressWrapper(indicator));
          if (!success) {
            ProgressIndicatorUtils.yieldToPendingWriteActions();
          }
        }
        catch (Throwable e) {
          promise.setError(e);
        }
      }
    });
    return promise;
  }

  private static void cancelAndRestartOnUserActivity(Promise<?> promise, ProgressIndicator indicator) {
    Disposable disposable = Disposable.newDisposable("Action Update");
    IdeEventQueueProxy.getInstance().addPostprocessor(e -> {
      if (e instanceof ComponentEvent && !(e instanceof PaintEvent) && (e.getID() & AWTEvent.MOUSE_MOTION_EVENT_MASK) == 0) {
        indicator.cancel();
      }
      return false;
    }, disposable);
    promise.onProcessed(__ -> Disposer.dispose(disposable));
  }

  private List<AnAction> doExpandActionGroup(ActionGroup group, boolean hideDisabled, UpdateStrategy strategy) {
    if (group instanceof XmlActionGroupStub) {
      throw new IllegalStateException("Trying to expand non-unstubbed group");
    }
    if (myAllowPartialExpand) {
      ProgressManager.checkCanceled();
    }

    Presentation presentation = update(group, strategy);
    if (presentation == null || !presentation.isVisible()) { // don't process invisible groups
      return Collections.emptyList();
    }

    List<AnAction> children = getGroupChildren(group, strategy);
    List<AnAction> result = ContainerUtil.concat(children, child -> expandGroupChild(child, hideDisabled, strategy));
    return group.postProcessVisibleChildren(result);
  }

  private List<AnAction> getGroupChildren(ActionGroup group, UpdateStrategy strategy) {
    return myGroupChildren.computeIfAbsent(group, __ -> {
      AnAction[] children = strategy.getChildren.apply(group);
      int nullIndex = ArrayUtil.indexOf(children, null);
      if (nullIndex < 0) return Arrays.asList(children);

      LOG.error("action is null: i=" + nullIndex + " group=" + group + " group id=" + myActionManager.getId(group));
      return ContainerUtil.filter(children, Conditions.notNull());
    });
  }

  private List<AnAction> expandGroupChild(AnAction child, boolean hideDisabled, UpdateStrategy strategy) {
    Presentation presentation = update(child, strategy);
    if (presentation == null) {
      return Collections.emptyList();
    }

    if (!presentation.isVisible() || (!presentation.isEnabled() && hideDisabled)) { // don't create invisible items in the menu
      return Collections.emptyList();
    }
    if (child instanceof ActionGroup) {
      ActionGroup actionGroup = (ActionGroup)child;
      JBIterable<AnAction> childrenIterable = iterateGroupChildren(actionGroup, strategy);
      if (!presentation.isVisible() || (!presentation.isEnabled() && hideDisabled)) {
        return Collections.emptyList();
      }

      boolean isPopup = actionGroup.isPopup(myPlace);
      boolean hasEnabled = false, hasVisible = false;
      if (hideDisabled || isPopup) {
        for (AnAction action : childrenIterable) {
          Presentation p = update(action, strategy);
          if (p == null) continue;
          hasVisible |= p.isVisible();
          hasEnabled |= p.isEnabled();
          // stop early if all the required flags are collected
          if (hasEnabled && hasVisible) break;
          if (hideDisabled && hasEnabled && !isPopup) break;
          if (isPopup && hasVisible && !hideDisabled) break;
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
          return Collections.singletonList(new EmptyAction.DelegatingCompactActionGroup((ActionGroup)child));
        }
        return Collections.singletonList(child);
      }

      return doExpandActionGroup((ActionGroup)child, hideDisabled || actionGroup instanceof CompactActionGroup, strategy);
    }

    return Collections.singletonList(child);
  }


  @Nonnull
  private JBIterable<AnAction> iterateGroupChildren(@Nonnull ActionGroup group, @Nonnull UpdateStrategy strategy) {
    boolean isDumb = myProject != null && DumbService.getInstance(myProject).isDumb();
    return JBTreeTraverser.<AnAction>from(o -> {
      if (o == group) return null;
      if (o instanceof AlwaysVisibleActionGroup) return null;
      if (isDumb && !o.isDumbAware()) return null;
      if (!(o instanceof ActionGroup)) return null;
      ActionGroup oo = (ActionGroup)o;
      Presentation presentation = update(oo, strategy);
      if (presentation == null || !presentation.isVisible()) {
        return null;
      }
      if ((oo.isPopup(myPlace) || strategy.canBePerformed.test(oo))) {
        return null;
      }
      return getGroupChildren(oo, strategy);
    }).withRoots(getGroupChildren(group, strategy))
      .unique()
      .traverse(TreeTraversal.LEAVES_DFS)
      .filter(o -> !(o instanceof AnSeparator) && !(isDumb && !o.isDumbAware()))
      .take(1000);
  }


  boolean canBePerformedCached(ActionGroup group) {
    return !Boolean.FALSE.equals(myCanBePerformedCache.get(group));
  }

  private boolean canBePerformed(ActionGroup group, UpdateStrategy strategy) {
    return myCanBePerformedCache.computeIfAbsent(group, __ -> strategy.canBePerformed.test(group));
  }

  private Presentation orDefault(AnAction action, Presentation presentation) {
    return presentation != null ? presentation : ActionUpdateEdtExecutor.computeOnEdt(() -> myFactory.getPresentation(action));
  }

  private static List<AnAction> removeUnnecessarySeparators(List<? extends AnAction> visible) {
    List<AnAction> result = new ArrayList<>();
    for (AnAction child : visible) {
      if (child instanceof AnSeparator) {
        if (!StringUtil.isEmpty(((AnSeparator)child).getText()) || (!result.isEmpty() && !(result.get(result.size() - 1) instanceof AnSeparator))) {
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
    AnActionEvent event = new AnActionEvent(null,
                                            getDataContext(action),
                                            myPlace,
                                            presentation,
                                            myActionManager,
                                            0,
                                            myContextMenuAction,
                                            myToolbarAction);
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
      final Project project = getDataContext(anAction).getData(Project.KEY);
      if (project != null && DumbService.getInstance(project).isDumb() && !anAction.isDumbAware()) {
        continue;
      }

      Presentation presentation = orDefault(anAction, update(anAction, strategy));
      if (anAction instanceof ActionGroup) {
        ActionGroup childGroup = (ActionGroup)anAction;

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

  // returns false if exception was thrown and handled
  boolean doUpdate(boolean isInModalContext, AnAction action, AnActionEvent e) {
    if (ApplicationManager.getApplication().isDisposed()) return false;

    long startTime = System.currentTimeMillis();
    final boolean result;
    try {
      result = !ActionUtil.performDumbAwareUpdate(action, e, false);
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

  private static class UpdateStrategy {
    final Function<AnAction, Presentation> update;
    final Function<ActionGroup, AnAction[]> getChildren;
    final Predicate<ActionGroup> canBePerformed;

    UpdateStrategy(Function<AnAction, Presentation> update,
                   Function<ActionGroup, AnAction[]> getChildren,
                   Predicate<ActionGroup> canBePerformed) {
      this.update = update;
      this.getChildren = getChildren;
      this.canBePerformed = canBePerformed;
    }
  }
}
