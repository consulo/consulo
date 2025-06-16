// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionGroupExpander;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.ex.action.*;
import consulo.util.concurrent.CancellablePromise;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Timer;
import java.util.*;

final class TBPanelActionGroup extends TBPanel {
    private static final boolean USE_CACHED_PRESENTATIONS = Boolean.getBoolean("touchbar.actions.use.cached.presentations");
    private static final int DELAY_FOR_CACHED_PRESENTATIONS_MS = Integer.getInteger("touchbar.actions.delay.for.cached.presentations", 750);
    private static final long DELAY_FOR_CACHED_PRESENTATIONS_NS = DELAY_FOR_CACHED_PRESENTATIONS_MS * 1000000L;

    private static final Logger LOG = Logger.getInstance(TBPanelActionGroup.class);
    private static final boolean IS_AUTOCLOSE_DISABLED = Boolean.getBoolean("touchbar.autoclose.disable");

    private final @Nonnull ActionGroup myActionGroup;
    private final @Nonnull PresentationFactory myFactory = new BasePresentationFactory();
    private final @Nullable Collection<AnAction> myAutoCloseActions;
    private final @Nullable Customizer myCustomizer;

    private final @Nonnull Updater myUpdateTimer = new Updater();
    private CancellablePromise<List<AnAction>> myLastUpdate;
    private long myLastUpdateNs = 0;
    private long myStartShowNs = 0;

    private final @Nonnull Map<AnAction, TBItemAnActionButton> myActionButtonPool = new HashMap<>();
    private final @Nonnull Map<Integer, TBItemGroup> myGroupPool = new HashMap<>();

    TBPanelActionGroup(@Nonnull String touchbarName,
                       @Nonnull ActionGroup actionGroup,
                       @Nullable Customizer customizations) {
        super(touchbarName, customizations != null ? customizations.getCrossEscInfo() : null, false);
        myActionGroup = actionGroup;

        // prepare list of AutoClose-actions
        if (!IS_AUTOCLOSE_DISABLED && customizations != null && customizations.getAutoCloseActionIds() != null) {
            final ActionManager actionManager = ActionManager.getInstance();
            final ArrayList<AnAction> autoCloseActions = new ArrayList<>();
            for (String actId : customizations.getAutoCloseActionIds()) {
                if (actId == null || actId.isEmpty()) {
                    continue;
                }
                final AnAction act = actionManager.getAction(actId);
                if (act == null) {
                    continue;
                }
                autoCloseActions.add(act);
            }

            validateAutoCloseActions(myActionGroup, autoCloseActions);

            if (LOG.isDebugEnabled() && !autoCloseActions.isEmpty()) {
                LOG.debug("initialized auto-closable touchbar '" + myName + "', auto-close actions: " + Arrays.toString(autoCloseActions.toArray()));
            }

            myAutoCloseActions = autoCloseActions.isEmpty() ? null : autoCloseActions;
        }
        else {
            myAutoCloseActions = null;
        }

        myCustomizer = customizations;
    }

    // returns true when some autoclose-action is disabled/hidden
    // used only to prevent showing of already autoclosed touchbars
    boolean updateAutoCloseAndCheck() {
        if (IS_AUTOCLOSE_DISABLED
            || myAutoCloseActions == null
            || myAutoCloseActions.isEmpty()
        ) {
            return false;
        }

        if (myUpdateTimer.isRunning()) {
            // simple performance protection: skip check for visible touchbars (they continuously invoke _checkAutoClose)
            return false;
        }

        // use 50-ms timeout to prevent freezes (just for insurance)
        final long startTimeMs = System.currentTimeMillis();
        final AnAction result = ProgressIndicatorUtils.withTimeout(50/*ms*/, () -> updateAutoCloseAndCheckImpl());
        long spentTimeMs = System.currentTimeMillis() - startTimeMs;
        if (spentTimeMs > 10) {
            LOG.debug("updateAutoCloseAndCheckImpl spent %d ms", spentTimeMs);
        }
        return result != null;
    }

    // returns action that is hidden or disabled
    AnAction updateAutoCloseAndCheckImpl() {
        final Application app = ApplicationManager.getApplication();
        if (app == null || app.isDisposed() || myAutoCloseActions == null) {
            return null;
        }

        for (AnAction action : myAutoCloseActions) {
            ProgressManager.checkCanceled();
            Presentation presentation = myFactory.getPresentation(action);
            presentation.setEnabledAndVisible(true);

            DataContext dataContext = DataManager.getInstance().getDataContext(Helpers.getCurrentFocusComponent());
            AnActionEvent event = new AnActionEvent(null,
                dataContext,
                ActionPlaces.TOUCHBAR_GENERAL,
                presentation,
                ActionManager.getInstance(),
                0,
                false,
                true
            );
            event.setInjectedContext(action.isInInjectedContext());

            final boolean result;
            try {
                result = !ActionUtil.performDumbAwareUpdate(action, event, false);
            }
            catch (Throwable exc) {
                continue;
            }
            if (!result) {
                continue;
            }
            if (!presentation.isEnabledAndVisible()) {
                return action;
            }
        }

        // all presentations of autoclose actions are successfully updated, and they are enabled and visible
        return null;
    }

    synchronized void startUpdateTimer() {
        if (myUpdateTimer.isRunning()) {
            return;
        }
        myStartShowNs = System.nanoTime();
        myUpdateTimer.start();
    }

    synchronized void stopUpdateTimer() {
        myUpdateTimer.stop();
        if (myLastUpdate != null) {
            myLastUpdate.cancel();
            myLastUpdate = null;
        }
    }

    @Override
    public synchronized void release() {
        super.release();

        final long startNs = myStats != null ? System.nanoTime() : 0;
        myActionButtonPool.forEach((act, item) -> item.releaseNativePeer());
        myActionButtonPool.clear();
        myGroupPool.forEach((n, item) -> item.releaseNativePeer());
        myGroupPool.clear();
        if (myStats != null) {
            myStats.incrementCounter(StatsCounters.touchbarReleaseDurationNs, System.nanoTime() - startNs);
        }

        myUpdateTimer.stop();
    }

    // https://developer.apple.com/design/human-interface-guidelines/macos/touch-bar/touch-bar-visual-design/
    //
    // Spacing type	Width between controls
    // Default	        16px
    // Small fixed space	32px
    // Large fixed space	64px
    // Flexible space	Varies. Matches the available space.
    private static final String ourSmallSeparatorText = "type.small";
    private static final String ourLargeSeparatorText = "type.large";
    private static final String ourFlexibleSeparatorText = "type.flexible";

    private void _rebuildButtons(@Nonnull List<? extends AnAction> actions) {
        //
        // clear all items
        //
        softClear();

        //
        // fill items with actions
        //

        List<AnAction> principal = null;
        TBItemGroup principalGroup = null;
        if (myCustomizer != null) {
            myCustomizer.prepare(myActionGroup);

            // collect actions from principal group (NSTouchbarGroupItem must be created with fixed list of items)
            for (AnAction action : actions) {
                if (myCustomizer.isPrincipalGroupAction(action)) {
                    if (principal == null) {
                        principal = new ArrayList<>();
                    }
                    principal.add(action);
                }
            }

            // create group item for principal actions
            if (principal != null) {
                principalGroup = myGroupPool.remove(principal.size());
                if (principalGroup == null) {
                    principalGroup = new TBItemGroup(myItems.toString(), myItemListener, principal);
                }
            }
        }

        int separatorCounter = 0;
        for (AnAction action : actions) {
            // 1. create separator
            // NOTE: we don't add separator into Main (or Principal) groups
            // just add separator into current list of actions
            if (action instanceof AnSeparator sep) {
                int increment = 1;
                if (sep.getText() != null) {
                    if (sep.getText().equals(ourSmallSeparatorText)) {
                        addSpacing(false);
                    }
                    else if (sep.getText().equals(ourLargeSeparatorText)) {
                        addSpacing(true);
                    }
                    else if (sep.getText().equals(ourFlexibleSeparatorText)) {
                        addFlexibleSpacing();
                    }
                }
                else {
                    separatorCounter += increment;
                }

                continue;
            } // separator

            if (separatorCounter > 0) {
                if (separatorCounter == 1) {
                    addSpacing(false);
                }
                else if (separatorCounter == 2) {
                    addSpacing(true);
                }
                else {
                    addFlexibleSpacing();
                }
                separatorCounter = 0;
            }

            // 2. find or create action button
            TBItemAnActionButton butt = null;

            // 2.1 check if current action from principal group
            if (principal != null) {
                int principalIndex = principal.indexOf(action);
                if (principalIndex >= 0) {
                    if (principalIndex == 0) {
                        addItem(principalGroup);
                        setPrincipal(principalGroup);
                    }
                    butt = principalGroup.getItem(principalIndex);
                    butt.setAnAction(action);
                }
            }

            // 2.2 create if wasn't found in principalGroup
            if (butt == null) {
                butt = createActionButton(action);
                addItem(butt);
            }

            // 3. update button with use of presentation
            final @Nonnull Presentation presentation = myFactory.getPresentation(action);

            butt.myIsVisible = presentation.isVisible();

            if (!butt.myIsVisible)
                continue;

            final long startNs = butt.actionStats != null ? System.nanoTime() : 0;
            butt.setDisabled(!presentation.isEnabled());

            boolean isSelected = false;
            if (butt.getAnAction() instanceof Toggleable) {
                isSelected = Toggleable.isSelected(presentation);
                butt.updateOptions |= NSTLibrary.BUTTON_UPDATE_FLAGS; // permanent update of toggleable-buttons (do we really need this ??)
            }
            butt.setSelected(isSelected);

            // 5. customize if necessary
            if (myCustomizer == null || !myCustomizer.applyCustomizations(butt, presentation)) {
                // Customizations weren't found, so use default view style: only icon (if presented, otherwise text)
                butt.setIconFromPresentation(presentation);
                final boolean hideText = butt.originIcon != null;
                final String text = hideText ? null : presentation.getText();
                butt.setText(text);
            }

            if (butt.actionStats != null)
                butt.actionStats.updateViewNs += System.nanoTime() - startNs;

            // 6. All visual data (img/text/flags) is set now, schedule async update for native peers (and collect buttons with updates)
            butt.updateLater(false);
        } // foreach action

        //
        // update visible items of native peer
        //
        selectVisibleItemsToShow();
    }

    // returns AnAction that caused auto-close
    // (some of AutoClose-actions was disabled or hidden)
    private AnAction _checkAutoClose(@Nonnull List<AnAction> actions) {
        if (IS_AUTOCLOSE_DISABLED
            || myAutoCloseActions == null
            || myAutoCloseActions.isEmpty()
            || actions.isEmpty()
        ) {
            return null;
        }

        for (AnAction autocloseAction : myAutoCloseActions) {
            // 1. if some of autoclose actions doesn't exist in actions then we must close this touchbar
            if (!actions.contains(autocloseAction)) {
                return autocloseAction;
            }

            // 2. if autoclose action is disabled (or hidden) then we must close this touchbar
            final @Nonnull Presentation presentation = myFactory.getPresentation(autocloseAction);
            if (!presentation.isVisible() || !presentation.isEnabled()) {
                return autocloseAction;
            }
        }

        return null;
    }

    private void _applyPresentationChanges(List<AnAction> actions) {
        final long startNs = System.nanoTime();

        if (actions == null) {
            return;
        }

        final AnAction autoCloseReason = _checkAutoClose(actions);
        if (autoCloseReason != null) {
            LOG.debug("touchbar '%s' was auto-closed because of: %s | %s", myName, autoCloseReason.getTemplateText(), autoCloseReason);
            _closeSelf();
        }

        _rebuildButtons(actions);

        if (myStats != null) {
            myStats.incrementCounter(StatsCounters.applyPresentationChangesDurationNs, System.nanoTime() - startNs);
        }
    }

    @Override
    @Nonnull
    TBItemAnActionButton createActionButton(@Nonnull AnAction action) {
        TBItemAnActionButton cached = myActionButtonPool.remove(action);
        if (cached != null) {
            cached.setAnAction(action);
            return cached;
        }

        // try find action with the same template-text
        for (Iterator<Map.Entry<AnAction, TBItemAnActionButton>> it = myActionButtonPool.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AnAction, TBItemAnActionButton> entry = it.next();
            final TBItemAnActionButton butt = entry.getValue();
            if (Objects.equals(action.getTemplateText(), butt.getAnAction().getTemplateText())) {
                it.remove();
                butt.setAnAction(action);
                return butt;
            }
        }

        return super.createActionButton(action);
    }

    void softClear() {
        if (myActionButtonPool.size() > 20) { // just simple protection for infinite growth
            synchronized (this) {
                myActionButtonPool.forEach((act, item) -> item.releaseNativePeer());
                myActionButtonPool.clear();
            }
        }
        myItems.softClear(myActionButtonPool, myGroupPool);
    }

    void updateActionItems() {
        if (!myUpdateTimer.isRunning()) {// don't update actions if was hidden
            return;
        }

        final long timeNs = System.nanoTime();
        final long elapsedFromStartShowNs = timeNs - myStartShowNs;
        myLastUpdateNs = timeNs;

        if (USE_CACHED_PRESENTATIONS && elapsedFromStartShowNs < DELAY_FOR_CACHED_PRESENTATIONS_NS) {
            // When user types text and presses modifier keys it causes to show alternative touchbar layouts, some of them are visible less than second.
            // To avoid unnecessary slow-update invocations for such bars we always try to use cached presentations for the first 500 ms
            if (myStats != null) {
                myStats.incrementCounter(StatsCounters.forceUseCached);
            }
            // start manual timer to update action-buttons if necessary
            final Timer t = new Timer(DELAY_FOR_CACHED_PRESENTATIONS_MS, e -> {
                if (System.nanoTime() - myLastUpdateNs > DELAY_FOR_CACHED_PRESENTATIONS_NS) { // update action-buttons if last update was long time ago
                    if (myStats != null) {
                        myStats.incrementCounter(StatsCounters.forceCachedDelayedUpdateCount);
                    }
                    updateActionItems();
                }
            });
            t.setRepeats(false);
            t.start();
            return;
        }

        if (myCustomizer != null) {
            myCustomizer.onBeforeActionsExpand(myActionGroup);
        }

        // NOTE: some of buttons (from dialogs for example) has custom component (used in _performAction, as event source (i.e. DataContext))
        // but here we expand actions with current-focus-component (theoretically it can cause that some actions will be updated incorrectly)
        DataContext dataContext = DataManager.getInstance().createAsyncDataContext(DataManager.getInstance().getDataContext(Helpers.getCurrentFocusComponent()));
        if (myLastUpdate != null) myLastUpdate.cancel();
        myLastUpdate = ActionGroupExpander.expandActionGroupAsync(false, myActionGroup, myFactory, dataContext, ActionPlaces.TOUCHBAR_GENERAL);
        myLastUpdate.onSuccess(actions -> _applyPresentationChanges(actions)).onProcessed(__ -> myLastUpdate = null);
        if (myStats != null) {
            myStats.incrementCounter(StatsCounters.totalUpdateDurationNs, System.nanoTime() - timeNs);
        }
    }

    private final class Updater {
        private @Nullable TimerListener myTimerImpl;

        void start() {
            if (myTimerImpl != null) {
                stop();
            }

            myTimerImpl = new TimerListener() {
                @Override
                public ModalityState getModalityState() {
                    return Application.get().getCurrentModalityState();
                }

                @Override
                public void run() {
                    updateActionItems();
                }
            };
            ActionManager.getInstance().addTimerListener(500, myTimerImpl);
        }

        void stop() {
            if (myTimerImpl == null) {
                return;
            }

            ActionManager.getInstance().removeTimerListener(myTimerImpl);
            myTimerImpl = null;
        }

        boolean isRunning() {
            return myTimerImpl != null;
        }
    }

    // check that all autoClose actions are presented in actionGroup
    private static void validateAutoCloseActions(@Nonnull ActionGroup actionGroup, @Nonnull Collection<AnAction> autoCloseActions) {
        Collection<AnAction> actionsFromGroup = new HashSet<>();
        Helpers.collectLeafActions(actionGroup, actionsFromGroup);
        if (!actionsFromGroup.containsAll(autoCloseActions)) {
            autoCloseActions.removeIf(a -> !actionsFromGroup.contains(a));
        }
    }
}
