// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.memory.MemoryViewManager;
import consulo.execution.debug.memory.MemoryViewManagerState;
import consulo.execution.debug.memory.TrackerForNewInstancesBase;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.event.MemoryViewManagerListener;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ClassesFilteredViewBase extends BorderLayoutPanel implements DataProvider, Disposable {
    protected static final double DELAY_BEFORE_INSTANCES_QUERY_COEFFICIENT = 0.5;
    protected static final double MAX_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(2);
    protected static final int DEFAULT_BATCH_SIZE = Integer.MAX_VALUE;
    private static final int INITIAL_TIME = 0;

    @Nonnull
    protected final Project myProject;
    protected final SingleAlarmWithMutableDelay mySingleAlarm;

    private final SearchTextField myFilterTextField = new FilterTextField();
    private final ClassesTable myTable;
    private final MyDebuggerSessionListener myDebugSessionListener;

    // tick on each session paused event
    private final AtomicInteger myTime = new AtomicInteger(INITIAL_TIME);

    private final AtomicInteger myLastUpdatingTime = new AtomicInteger(myTime.intValue());

    /**
     * Indicates that view is visible
     */
    protected volatile boolean myIsActive;

    public ClassesFilteredViewBase(@Nonnull XDebugSession debugSession) {
        myProject = debugSession.getProject();

        debugSession.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                debugSession.removeSessionListener(this);
            }
        });

        MemoryViewManagerState memoryViewManagerState = MemoryViewManager.getInstance().getState();

        myTable = createClassesTable(memoryViewManagerState);
        myTable.getEmptyText().setText(XDebuggerLocalize.memoryViewEmptyRunning());
        Disposer.register(this, myTable);


        myTable.addKeyListener(new KeyAdapter() {
            @Override
            @RequiredUIAccess
            public void keyTyped(KeyEvent e) {
                char keyChar = e.getKeyChar();
                if (KeyboardUtils.isEnterKey(keyChar)) {
                    handleClassSelection(myTable.getSelectedClass());
                }
                else if (KeyboardUtils.isPartOfJavaClassName(keyChar) || KeyboardUtils.isBackSpace(keyChar)) {
                    String text = myFilterTextField.getText();
                    String newText = KeyboardUtils.isBackSpace(keyChar)
                        ? text.substring(0, text.length() - 1)
                        : text + keyChar;
                    myFilterTextField.setText(newText);

                    ApplicationIdeFocusManager manager = ApplicationIdeFocusManager.getInstance();
                    manager.getInstanceForProject(myProject).requestFocus(myFilterTextField, false);
                }
            }
        });

        myFilterTextField.addKeyboardListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                dispatch(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                dispatch(e);
            }

            private void dispatch(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (myTable.isInClickableMode() && (KeyboardUtils.isPartOfJavaClassName(e.getKeyChar()) || KeyboardUtils.isEnterKey(keyCode))) {
                    myTable.exitClickableMode();
                    updateClassesAndCounts(true);
                }
                else if (KeyboardUtils.isUpDownKey(keyCode) || KeyboardUtils.isEnterKey(keyCode)) {
                    myTable.dispatchEvent(e);
                }
            }
        });

        myFilterTextField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@Nonnull DocumentEvent e) {
                myTable.setFilterPattern(myFilterTextField.getText());
            }
        });

        MemoryViewManagerListener memoryViewManagerListener = state -> {
            myTable.setFilteringByDiffNonZero(state.isShowWithDiffOnly);
            myTable.setFilteringByInstanceExists(state.isShowWithInstancesOnly);
            myTable.setFilteringByTrackingState(state.isShowTrackedOnly);
            if (state.isAutoUpdateModeOn && myTable.isInClickableMode()) {
                updateClassesAndCounts(true);
            }
        };

        MemoryViewManager.getInstance().addMemoryViewManagerListener(memoryViewManagerListener, this);

        myDebugSessionListener = new MyDebuggerSessionListener();
        debugSession.addSessionListener(myDebugSessionListener, this);

        mySingleAlarm = new SingleAlarmWithMutableDelay(
            suspendContext -> {
                myProject.getApplication().invokeLater(() -> myTable.setBusy(true));
                scheduleUpdateClassesCommand(suspendContext);
            },
            this
        );

        mySingleAlarm.setDelay((int) TimeUnit.MILLISECONDS.toMillis(500));

        PopupHandler.installPopupHandler(myTable, "MemoryView.ClassesPopupActionGroup", "MemoryView.ClassesPopupActionGroup");

        JScrollPane scroll = ScrollPaneFactory.createScrollPane(myTable, SideBorder.TOP);
        DefaultActionGroup group = (DefaultActionGroup) ActionManager.getInstance().getAction("MemoryView.SettingsPopupActionGroup");
        group.setPopup(true);

        ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, ActionGroup.newImmutableBuilder().add(group).build(), true);
        toolbar.setMiniMode(true);
        toolbar.setTargetComponent(this);

        BorderLayoutPanel topPanel = new BorderLayoutPanel();
        topPanel.addToCenter(myFilterTextField);
        topPanel.addToRight(toolbar.getComponent());
        addToTop(topPanel);
        addToCenter(scroll);
    }

    public JComponent getDefaultFocusedComponent() {
        return myFilterTextField;
    }

    @Nonnull
    protected ClassesTable createClassesTable(MemoryViewManagerState memoryViewManagerState) {
        return new ClassesTable(myProject, this, memoryViewManagerState.isShowWithDiffOnly,
            memoryViewManagerState.isShowWithInstancesOnly, memoryViewManagerState.isShowTrackedOnly
        );
    }

    protected abstract void scheduleUpdateClassesCommand(XSuspendContext context);

    @Nullable
    protected TrackerForNewInstancesBase getStrategy(@Nonnull TypeInfo ref) {
        return null;
    }

    @RequiredUIAccess
    protected void handleClassSelection(@Nullable TypeInfo ref) {
        XDebugSession debugSession = XDebuggerManager.getInstance(myProject).getCurrentSession();
        if (ref != null && debugSession != null && debugSession.isSuspended()) {
            if (!ref.canGetInstanceInfo()) {
                NotificationService.getInstance()
                    .newInfo(XDebuggerUIConstants.NOTIFICATION_GROUP)
                    .content(XDebuggerLocalize.memoryUnableToGetInstancesOfClass(ref.name()))
                    .notify(debugSession.getProject());
                return;
            }

            getInstancesWindow(ref, debugSession).show();
        }
    }

    protected abstract InstancesWindowBase getInstancesWindow(@Nonnull TypeInfo ref, XDebugSession debugSession);

    protected void updateClassesAndCounts(boolean immediate) {
        myProject.getApplication().invokeLater(
            () -> {
                XDebugSession debugSession = XDebuggerManager.getInstance(myProject).getCurrentSession();
                if (shouldBeUpdated(debugSession)) {
                    XSuspendContext suspendContext = debugSession.getSuspendContext();
                    if (suspendContext != null) {
                        if (immediate) {
                            mySingleAlarm.cancelAndRequestImmediate(suspendContext);
                        }
                        else {
                            mySingleAlarm.cancelAndRequest(suspendContext);
                        }
                    }
                }
            },
            myProject.getDisposed()
        );
    }

    @Contract("null -> false")
    private boolean shouldBeUpdated(@Nullable XDebugSession session) {
        if (session instanceof XDebugSessionImpl && session.isReadOnly()) {
            // update memory view only once (initially) if session is in read-only mode
            return myLastUpdatingTime.get() == INITIAL_TIME;
        }

        return session != null;
    }

    protected void doActivate() {
        myDebugSessionListener.setActive(true);

        if (isContentObsolete()) {
            if (MemoryViewManager.getInstance().isAutoUpdateModeEnabled()) {
                updateClassesAndCounts(true);
            }
            else {
                makeTableClickable();
            }
        }
    }

    private void makeTableClickable() {
        myProject.getApplication().invokeLater(() -> myTable.makeClickable(() -> updateClassesAndCounts(true)));
    }

    protected void doPause() {
        myDebugSessionListener.setActive(false);
        mySingleAlarm.cancelAllRequests();
    }

    private boolean isContentObsolete() {
        return myLastUpdatingTime.get() != myTime.get() && shouldBeUpdated(XDebuggerManager.getInstance(myProject).getCurrentSession());
    }

    protected void viewUpdated() {
        myLastUpdatingTime.set(myTime.get());
    }

    public ClassesTable getTable() {
        return myTable;
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        return null;
    }

    private static class FilterTextField extends SearchTextField {
        FilterTextField() {
            super(false);
        }

        @Override
        protected void showPopup() {
        }
    }

    @Nullable
    protected XDebugSessionListener getAdditionalSessionListener() {
        return null;
    }

    private class MyDebuggerSessionListener implements XDebugSessionListener {
        private volatile boolean myIsActive = false;

        void setActive(boolean value) {
            myIsActive = value;
        }

        @Override
        public void sessionResumed() {
            if (myIsActive) {
                XDebugSessionListener additionalSessionListener = getAdditionalSessionListener();
                if (additionalSessionListener != null) {
                    additionalSessionListener.sessionResumed();
                }
                myProject.getApplication().invokeLater(() -> myTable.hideContent(XDebuggerLocalize.memoryViewEmptyRunning().get()));

                mySingleAlarm.cancelAllRequests();
            }
        }

        @Override
        public void sessionStopped() {
            XDebugSessionListener additionalSessionListener = getAdditionalSessionListener();
            if (additionalSessionListener != null) {
                additionalSessionListener.sessionStopped();
            }
            mySingleAlarm.cancelAllRequests();
            myProject.getApplication().invokeLater(() -> myTable.clean(XDebuggerLocalize.memoryViewEmptyStopped().get()));
        }

        @Override
        public void sessionPaused() {
            myTime.incrementAndGet();
            XDebugSessionListener additionalSessionListener = getAdditionalSessionListener();
            if (additionalSessionListener != null) {
                additionalSessionListener.sessionPaused();
            }
            if (myIsActive && isContentObsolete()) {
                if (MemoryViewManager.getInstance().isAutoUpdateModeEnabled()) {
                    updateClassesAndCounts(false);
                }
                else {
                    makeTableClickable();
                }
            }
        }
    }
}
