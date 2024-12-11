/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.frame;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XStackFrameContainerEx;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.PopupMenuListenerAdapter;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author nik
 */
public class XFramesView extends XDebugView {
    private static final Logger LOG = Logger.getInstance(XFramesView.class);
    public static final Key<XFramesView> DATA_KEY = Key.create("XDEBUGGER_FRAMES_VIEW");

    private final JPanel myMainPanel;
    private final XDebuggerFramesList myFramesList;
    private final ComboBox<XExecutionStack> myThreadComboBox;
    private final Object2IntMap<XExecutionStack> myExecutionStacksWithSelection = new Object2IntOpenHashMap<>();
    private XExecutionStack mySelectedStack;
    private int mySelectedFrameIndex;
    private Rectangle myVisibleRect;
    private boolean myListenersEnabled;
    private final Map<XExecutionStack, StackFramesListBuilder> myBuilders = new HashMap<>();
    private final Wrapper myThreadsPanel;
    private boolean myThreadsCalculated = false;
    private boolean myRefresh = false;

    public XFramesView(@Nonnull Project project) {
        myMainPanel = new JPanel(new BorderLayout());

        myFramesList = new XDebuggerFramesList(project);
        myFramesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (myListenersEnabled && !e.getValueIsAdjusting() && mySelectedFrameIndex != myFramesList.getSelectedIndex()) {
                    processFrameSelection(getSession(e), true);
                }
            }
        });
        myFramesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (myListenersEnabled) {
                    int i = myFramesList.locationToIndex(e.getPoint());
                    if (i != -1 && myFramesList.isSelectedIndex(i)) {
                        processFrameSelection(getSession(e), true);
                    }
                }
            }
        });

        myFramesList.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(final Component comp, final int x, final int y) {
                ActionManager actionManager = ActionManager.getInstance();
                ActionGroup group = (ActionGroup) actionManager.getAction(XDebuggerActions.FRAMES_TREE_POPUP_GROUP);
                actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group).getComponent().show(comp, x, y);
            }
        });

        myMainPanel.add(ScrollPaneFactory.createScrollPane(myFramesList), BorderLayout.CENTER);

        myThreadComboBox = new ComboBox<>();
        myThreadComboBox.putClientProperty("FlatLaf.style", "borderColor: #0000; disabledBorderColor: #0000; borderWidth: 0; focusWidth: 0; innerFocusWidth: 1; innerOutlineWidth: 1; arc: 0");
        myThreadComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, XExecutionStack value, int index, boolean selected, boolean hasFocus) {
                setBorder(JBCurrentTheme.listCellBorderFull());
                
                if (value != null) {
                    append(value.getDisplayName());
                    setIcon(value.getIcon());
                }
            }
        });
        
        myThreadComboBox.addItemListener(e -> {
            if (!myListenersEnabled) {
                return;
            }

            if (e.getStateChange() == ItemEvent.SELECTED) {
                Object item = e.getItem();
                if (item != mySelectedStack && item instanceof XExecutionStack) {
                    XDebugSession session = getSession(e);
                    if (session != null) {
                        myRefresh = false;
                        updateFrames((XExecutionStack) item, session, null);
                    }
                }
            }
        });
        myThreadComboBox.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                XDebugSession session = getSession(e);
                XSuspendContext context = session == null ? null : session.getSuspendContext();
                if (context != null && !myThreadsCalculated) {
                    myThreadsCalculated = true;
                    //noinspection unchecked
                    myThreadComboBox.addItem(null); // rendered as "Loading..."
                    context.computeExecutionStacks(new XSuspendContext.XExecutionStackContainer() {
                        @Override
                        public void addExecutionStack(@Nonnull final List<? extends XExecutionStack> executionStacks, boolean last) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                addExecutionStacks(executionStacks);
                                if (last) {
                                    myThreadComboBox.removeItem(null);
                                    ComboPopup popup = myThreadComboBox.getPopup();
                                    if (popup != null && popup.isVisible()) {
                                        popup.hide();
                                        popup.show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void errorOccurred(@Nonnull String errorMessage) {
                        }
                    });
                }
            }
        });
        new ComboboxSpeedSearch(myThreadComboBox) {
            @Override
            protected String getElementText(Object element) {
                return ((XExecutionStack) element).getDisplayName();
            }
        };

        myThreadsPanel = new Wrapper();
        myThreadsPanel.setBorder(new CustomLineBorder(UIUtil.getBorderColor(), 0, 0, 1, 0));
        myMainPanel.add(myThreadsPanel, BorderLayout.NORTH);
    }

    public void selectFrame(XExecutionStack stack, XStackFrame frame) {
        myThreadComboBox.setSelectedItem(stack);

        Application.get().invokeLater(() -> myFramesList.setSelectedValue(frame, true));
    }

    private StackFramesListBuilder getOrCreateBuilder(XExecutionStack executionStack, XDebugSession session) {
        return myBuilders.computeIfAbsent(executionStack, k -> new StackFramesListBuilder(executionStack, session));
    }

    @Override
    public void processSessionEvent(@Nonnull SessionEvent event, @Nonnull XDebugSession session) {
        myRefresh = event == SessionEvent.SETTINGS_CHANGED;

        if (event == SessionEvent.BEFORE_RESUME) {
            return;
        }

        XExecutionStack currentExecutionStack = ((XDebugSessionImpl) session).getCurrentExecutionStack();
        XStackFrame currentStackFrame = session.getCurrentStackFrame();
        XSuspendContext suspendContext = session.getSuspendContext();

        if (event == SessionEvent.FRAME_CHANGED && Objects.equals(mySelectedStack, currentExecutionStack)) {
            ApplicationManager.getApplication().assertIsDispatchThread();
            if (currentStackFrame != null) {
                myFramesList.setSelectedValue(currentStackFrame, true);
                mySelectedFrameIndex = myFramesList.getSelectedIndex();
                myExecutionStacksWithSelection.put(mySelectedStack, mySelectedFrameIndex);
            }
            return;
        }

        Application.get().invokeLater(() -> {
            if (event != SessionEvent.SETTINGS_CHANGED) {
                mySelectedFrameIndex = 0;
                mySelectedStack = null;
                myVisibleRect = null;
            }
            else {
                myVisibleRect = myFramesList.getVisibleRect();
            }

            myListenersEnabled = false;
            myBuilders.values().forEach(StackFramesListBuilder::dispose);
            myBuilders.clear();

            if (suspendContext == null) {
                requestClear();
                return;
            }

            if (event == SessionEvent.PAUSED) {
                // clear immediately
                cancelClear();
                clear();
            }

            XExecutionStack activeExecutionStack = mySelectedStack != null ? mySelectedStack : currentExecutionStack;
            addExecutionStacks(Collections.singletonList(activeExecutionStack));

            XExecutionStack[] executionStacks = suspendContext.getExecutionStacks();
            addExecutionStacks(Arrays.asList(executionStacks));

            myThreadComboBox.setSelectedItem(activeExecutionStack);
            myThreadsPanel.removeAll();
            final boolean invisible = executionStacks.length == 1 && StringUtil.isEmpty(executionStacks[0].getDisplayName());
            if (!invisible) {
                myThreadsPanel.add(myThreadComboBox, BorderLayout.CENTER);
            }
            updateFrames(activeExecutionStack, session, event == SessionEvent.FRAME_CHANGED ? currentStackFrame : null);
        });
    }

    @Override
    protected void clear() {
        myThreadComboBox.removeAllItems();
        myFramesList.clear();
        myThreadsCalculated = false;
        myExecutionStacksWithSelection.clear();
    }

    private void addExecutionStacks(List<? extends XExecutionStack> executionStacks) {
        for (XExecutionStack executionStack : executionStacks) {
            if (!myExecutionStacksWithSelection.containsKey(executionStack)) {
                //noinspection unchecked
                myThreadComboBox.addItem(executionStack);
                myExecutionStacksWithSelection.put(executionStack, 0);
            }
        }
    }

    private void updateFrames(XExecutionStack executionStack, @Nonnull XDebugSession session, @Nullable XStackFrame frameToSelect) {
        if (mySelectedStack != null) {
            getOrCreateBuilder(mySelectedStack, session).stop();
        }

        mySelectedStack = executionStack;
        if (executionStack != null) {
            mySelectedFrameIndex = myExecutionStacksWithSelection.getOrDefault(executionStack, 0);
            StackFramesListBuilder builder = getOrCreateBuilder(executionStack, session);
            builder.setToSelect(frameToSelect != null ? frameToSelect : mySelectedFrameIndex);
            myListenersEnabled = false;
            builder.initModel(myFramesList.getModel());
            myListenersEnabled = !builder.start();
        }
    }

    @Override
    public void dispose() {
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    private void processFrameSelection(XDebugSession session, boolean force) {
        mySelectedFrameIndex = myFramesList.getSelectedIndex();
        myExecutionStacksWithSelection.put(mySelectedStack, mySelectedFrameIndex);

        Object selected = myFramesList.getSelectedValue();
        if (selected instanceof XStackFrame) {
            if (session != null) {
                if (force || (!myRefresh && session.getCurrentStackFrame() != selected)) {
                    session.setCurrentStackFrame(mySelectedStack, (XStackFrame) selected, mySelectedFrameIndex == 0);
                }
            }
        }
    }

    private class StackFramesListBuilder implements XStackFrameContainerEx {
        private XExecutionStack myExecutionStack;
        private final List<XStackFrame> myStackFrames;
        private String myErrorMessage;
        private int myNextFrameIndex = 0;
        private volatile boolean myRunning;
        private boolean myAllFramesLoaded;
        private final XDebugSession mySession;
        private Object myToSelect;

        private StackFramesListBuilder(final XExecutionStack executionStack, XDebugSession session) {
            myExecutionStack = executionStack;
            mySession = session;
            myStackFrames = new ArrayList<>();
        }

        void setToSelect(Object toSelect) {
            myToSelect = toSelect;
        }

        @Override
        public void addStackFrames(@Nonnull final List<? extends XStackFrame> stackFrames, final boolean last) {
            addStackFrames(stackFrames, null, last);
        }

        @Override
        public void addStackFrames(@Nonnull final List<? extends XStackFrame> stackFrames, @Nullable XStackFrame toSelect, final boolean last) {
            if (isObsolete()) {
                return;
            }
            Application.get().invokeLater(() -> {
                if (isObsolete()) {
                    return;
                }
                myStackFrames.addAll(stackFrames);
                addFrameListElements(stackFrames, last);

                if (toSelect != null) {
                    setToSelect(toSelect);
                }

                myNextFrameIndex += stackFrames.size();
                myAllFramesLoaded = last;

                selectCurrentFrame();

                if (last) {
                    if (myVisibleRect != null) {
                        myFramesList.scrollRectToVisible(myVisibleRect);
                    }
                    myRunning = false;
                    myListenersEnabled = true;
                }
            });
        }

        @Override
        public void errorOccurred(@Nonnull final String errorMessage) {
            if (isObsolete()) {
                return;
            }
            Application.get().invokeLater(() -> {
                if (isObsolete()) {
                    return;
                }
                if (myErrorMessage == null) {
                    myErrorMessage = errorMessage;
                    addFrameListElements(Collections.singletonList(errorMessage), true);
                    myRunning = false;
                    myListenersEnabled = true;
                }
            });
        }

        private void addFrameListElements(final List<?> values, final boolean last) {
            if (myExecutionStack != null && myExecutionStack == mySelectedStack) {
                DefaultListModel model = myFramesList.getModel();
                int insertIndex = model.size();
                boolean loadingPresent = !model.isEmpty() && model.getElementAt(model.getSize() - 1) == null;
                if (loadingPresent) {
                    insertIndex--;
                }
                for (Object value : values) {
                    //noinspection unchecked
                    model.add(insertIndex++, value);
                }
                if (last) {
                    if (loadingPresent) {
                        model.removeElementAt(model.getSize() - 1);
                    }
                }
                else if (!loadingPresent) {
                    //noinspection unchecked
                    model.addElement(null);
                }
                myFramesList.repaint();
            }
        }

        @Override
        public boolean isObsolete() {
            return !myRunning;
        }

        public void dispose() {
            myRunning = false;
            myExecutionStack = null;
        }

        public boolean start() {
            if (myExecutionStack == null || myErrorMessage != null) {
                return false;
            }
            myRunning = true;
            myStackFrames.clear();
            myExecutionStack.computeStackFrames(this);
            return true;
        }

        public void stop() {
            myRunning = false;
        }

        private void selectCurrentFrame() {
            if (myToSelect instanceof XStackFrame) {
                if (!Objects.equals(myFramesList.getSelectedValue(), myToSelect) && myFramesList.getModel().contains(myToSelect)) {
                    myFramesList.setSelectedValue(myToSelect, true);
                    processFrameSelection(mySession, false);
                    myListenersEnabled = true;
                }
                if (myAllFramesLoaded && myFramesList.getSelectedValue() == null) {
                    LOG.error("Frame was not found, " + myToSelect.getClass() + " must correctly override equals");
                }
            }
            else if (myToSelect instanceof Integer) {
                int selectedFrameIndex = (int) myToSelect;
                if (myFramesList.getSelectedIndex() != selectedFrameIndex &&
                    myFramesList.getElementCount() > selectedFrameIndex &&
                    myFramesList.getModel().get(selectedFrameIndex) != null) {
                    myFramesList.setSelectedIndex(selectedFrameIndex);
                    processFrameSelection(mySession, false);
                    myListenersEnabled = true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void initModel(final DefaultListModel model) {
            model.removeAllElements();
            myStackFrames.forEach(model::addElement);
            if (myErrorMessage != null) {
                model.addElement(myErrorMessage);
            }
            else if (!myAllFramesLoaded) {
                model.addElement(null);
            }
            selectCurrentFrame();
        }
    }
}
