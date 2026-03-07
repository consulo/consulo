// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.util.concurrent.CancellablePromise;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarUpdateQueue extends MergingUpdateQueue {
    private final AtomicBoolean myModelUpdating = new AtomicBoolean(Boolean.FALSE);
    private final Alarm myUserActivityAlarm = new Alarm(this);
    private Runnable myRunWhenListRebuilt;
    private final Runnable myUserActivityAlarmRunnable = this::processUserActivity;

    private final NavBarPanel myPanel;

    // Async pipeline: background task tracking and pre-computed item data cache
    private volatile CancellablePromise<?> myBackgroundTask;
    private volatile List<NavBarItemData> myItemDataCache;

    public NavBarUpdateQueue(NavBarPanel panel) {
        super("NavBar", 100, true, panel, panel);
        myPanel = panel;
        setTrackUiActivity(true);
        IdeEventQueueProxy.getInstance().addActivityListener(this::restartRebuild, panel);
    }

    private void requestModelUpdate(@Nullable final DataContext context, final @Nullable Object object, boolean requeue) {
        if (myModelUpdating.getAndSet(true) && !requeue) {
            return;
        }

        cancelAllUpdates();

        // Cancel in-flight background task from previous request
        CancellablePromise<?> prev = myBackgroundTask;
        if (prev != null) {
            prev.cancel();
            myBackgroundTask = null;
        }

        queue(new AfterModelUpdate(ID.MODEL) {
            @Override
            public void run() {
                if (context != null || object != null) {
                    requestModelUpdateFromContextOrObject(context, object);
                }
                else {
                    DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> requestModelUpdateFromContextOrObject(dataContext, null));
                }
            }

            @Override
            public void setRejected() {
                super.setRejected();
                myModelUpdating.set(false);
            }
        });
    }

    private void requestModelUpdateFromContextOrObject(DataContext dataContext, Object object) {
        NavBarModel model = myPanel.getModel();

        if (dataContext != null) {
            if (dataContext.getData(Project.KEY) != myPanel.getProject() || myPanel.isNodePopupActive()) {
                requestModelUpdate(null, myPanel.getContextObject(), true);
                return;
            }
        }

        // Phase A (EDT): extract context — lightweight, no ReadAction needed
        NavBarModel.ContextResult ctx = null;
        if (dataContext != null) {
            Window window = SwingUtilities.getWindowAncestor(myPanel);
            DataContext effectiveCtx;
            if (window != null && !window.isFocused()) {
                effectiveCtx = DataManager.getInstance().getDataContext(myPanel);
            }
            else {
                effectiveCtx = dataContext;
            }
            ctx = model.extractFromContext(effectiveCtx);

            // extractFromContext returns all nulls when no update is needed
            if (ctx.element() == null && ctx.root() == null) {
                myModelUpdating.set(false);
                return;
            }
        }

        if (ctx == null && object == null) {
            myModelUpdating.set(false);
            return;
        }

        // Phase B (Background): build model + compute presentation inside ReadAction.nonBlocking()
        NavBarPresentation presentation = myPanel.getPresentation();
        NavBarModel.ContextResult finalCtx = ctx;
        Object finalObject = object;

        CancellablePromise<?> promise = ReadAction.nonBlocking(() -> {
                List<NavBarItemData> items;
                if (finalCtx != null) {
                    items = model.buildModelAndPresentation(finalCtx.element(), finalCtx.ownerExtension(), finalCtx.root(), presentation);
                }
                else {
                    items = model.buildModelAndPresentation(null, null, finalObject, presentation);
                }

                return Map.<List<NavBarItemData>, List<Object>>entry(items, items.stream().map(NavBarItemData::element).toList());
            })
            .expireWith(myPanel)
            .finishOnUiThread(e -> {
                model.applyModel(e.getValue());
                myItemDataCache = e.getKey();
                myModelUpdating.set(false);
                queueRebuildUi();
            })
            .submit(AppExecutorUtil.getAppExecutorService());

        myBackgroundTask = promise;

        // If background task fails (not cancelled), clear the updating flag
        promise.onError(e -> myModelUpdating.set(false));
        // Note: myModelUpdating stays true until Phase C completes,
        // so AfterModelUpdate for UI/REVALIDATE will re-queue themselves until then
    }

    void restartRebuild() {
        myUserActivityAlarm.cancelAllRequests();
        if (!myUserActivityAlarm.isDisposed()) {
            myUserActivityAlarm.addRequest(myUserActivityAlarmRunnable, Registry.intValue("navBar.userActivityMergeTime"));
        }
    }

    private void processUserActivity() {
        if (myPanel == null || !myPanel.isShowing()) {
            return;
        }

        Project project = myPanel.getProject();
        ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> {
            Window wnd = SwingUtilities.windowForComponent(myPanel);
            if (wnd == null) {
                return;
            }

            Component focus = null;

            if (!wnd.isActive()) {
                IdeFrame frame = DesktopIdeFrameUtil.findIdeFrameFromParent(myPanel);
                if (frame != null) {
                    focus = ProjectIdeFocusManager.getInstance(project).getLastFocusedFor(frame);
                }
            }
            else {
                Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
                if (window instanceof Dialog dialog && dialog.isModal() && !SwingUtilities.isDescendingFrom(myPanel, dialog)) {
                    return;
                }
            }

            if (focus != null && focus.isShowing()) {
                if (!myPanel.isFocused() && !myPanel.isNodePopupActive()) {
                    requestModelUpdate(DataManager.getInstance().getDataContext(focus), null, false);
                }
            }
            else if (wnd.isActive()) {
                if (myPanel.allowNavItemsFocus() && (myPanel.isFocused() || myPanel.isNodePopupActive())) {
                    return;
                }
                requestModelUpdate(null, myPanel.getContextObject(), false);
            }
        });
    }

    public void queueModelUpdate(DataContext context) {
        requestModelUpdate(context, null, false);
    }

    public void queueModelUpdateFromFocus() {
        queueModelUpdateFromFocus(false);
    }

    public void queueModelUpdateFromFocus(boolean requeue) {
        requestModelUpdate(null, myPanel.getContextObject(), requeue);
    }

    public void queueModelUpdateForObject(Object object) {
        requestModelUpdate(null, object, false);
    }


    public void queueRebuildUi() {
        queue(new AfterModelUpdate(ID.UI) {
            @Override
            protected void after() {
                rebuildUi();
            }
        });
        queueRevalidate(null);
    }

    public void rebuildUi() {
        List<NavBarItemData> data = myItemDataCache;
        if (data != null) {
            // New async path: use pre-computed data — no ReadAction needed
            myItemDataCache = null;

            myPanel.clearItems();
            for (int i = 0; i < data.size(); i++) {
                NavBarItem label = new NavBarItem(myPanel, data.get(i), i, null);
                myPanel.installActions(i, label);
                myPanel.addItem(label);
            }
        }
        else {
            // Legacy path (for popup hint, direct calls)
            if (!myPanel.isRebuildUiNeeded()) {
                return;
            }

            myPanel.clearItems();
            for (int index = 0; index < myPanel.getModel().size(); index++) {
                Object object = myPanel.getModel().get(index);
                NavBarItem label = new NavBarItem(myPanel, object, index, null);
                myPanel.installActions(index, label);
                myPanel.addItem(label);
            }
        }

        rebuildComponent();

        if (myRunWhenListRebuilt != null) {
            Runnable r = myRunWhenListRebuilt;
            myRunWhenListRebuilt = null;
            r.run();
        }
    }


    private void rebuildComponent() {
        myPanel.removeAll();

        for (NavBarItem item : myPanel.getItems()) {
            myPanel.add(item);
        }

        myPanel.revalidate();
        myPanel.repaint();

        queueAfterAll(myPanel::scrollSelectionToVisible, ID.SCROLL_TO_VISIBLE);
    }

    private void queueRevalidate(@Nullable final Runnable after) {
        queue(new AfterModelUpdate(ID.REVALIDATE) {
            @Override
            protected void after() {
                LightweightHintImpl hint = myPanel.getHint();
                if (hint != null) {
                    myPanel.getHintContainerShowPoint().doWhenDone(relativePoint -> {
                        hint.setSize(myPanel.getPreferredSize());
                        hint.setLocation(relativePoint);
                        if (after != null) {
                            after.run();
                        }
                    });
                }
                else {
                    if (after != null) {
                        after.run();
                    }
                }
            }
        });
    }

    void queueSelect(final Runnable runnable) {
        queue(new AfterModelUpdate(NavBarUpdateQueue.ID.SELECT) {
            @Override
            protected void after() {
                runnable.run();
            }
        });
    }

    void queueAfterAll(final Runnable runnable, ID id) {
        queue(new AfterModelUpdate(id) {
            @Override
            protected void after() {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    boolean isUpdating() {
        return myModelUpdating.get();
    }

    private abstract class AfterModelUpdate extends Update {
        private AfterModelUpdate(ID id) {
            super(id.name(), id.getPriority());
        }

        @Override
        public void run() {
            if (myModelUpdating.get()) {
                queue(this);
            }
            else {
                after();
            }
        }

        protected void after() {
        }
    }

    public enum ID {
        MODEL(0),
        UI(1),
        REVALIDATE(2),
        SELECT(3),
        SCROLL_TO_VISIBLE(4),
        SHOW_HINT(4),
        REQUEST_FOCUS(4),
        NAVIGATE_INSIDE(4),
        TYPE_AHEAD_FINISHED(5);

        private final int myPriority;

        ID(int priority) {
            myPriority = priority;
        }

        public int getPriority() {
            return myPriority;
        }
    }
}
