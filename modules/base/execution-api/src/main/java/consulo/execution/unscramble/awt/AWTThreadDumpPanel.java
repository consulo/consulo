/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.unscramble.awt;

import consulo.application.AllIcons;
import consulo.execution.internal.ExecutionNotificationGroupHolder;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.unscramble.AnalyzeStacktraceUtil;
import consulo.execution.unscramble.ThreadDumpPanel;
import consulo.execution.unscramble.ThreadOperation;
import consulo.execution.unscramble.ThreadState;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Jeka
 * @author Konstantin Bulenkov
 */
public class AWTThreadDumpPanel extends JPanel implements ThreadDumpPanel {
    private final JBList<ThreadState> myThreadList;

    public AWTThreadDumpPanel(Project project, final ConsoleView consoleView, final DefaultActionGroup toolbarActions, final List<ThreadState> threadDump) {
        super(new BorderLayout());
        final ThreadState[] data = threadDump.toArray(new ThreadState[threadDump.size()]);
        DefaultListModel<ThreadState> model = new DefaultListModel();
        for (ThreadState threadState : data) {
            model.addElement(threadState);
        }
        myThreadList = new JBList<>(model);
        myThreadList.setCellRenderer(new ThreadListCellRenderer());
        myThreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myThreadList.addListSelectionListener(e -> {
            int index = myThreadList.getSelectedIndex();
            if (index >= 0) {
                ThreadState selection = myThreadList.getModel().getElementAt(index);
                AnalyzeStacktraceUtil.printStacktrace(consoleView, selection.getStackTrace());
            }
            else {
                AnalyzeStacktraceUtil.printStacktrace(consoleView, "");
            }
            myThreadList.repaint();
        });
        toolbarActions.add(new CopyToClipboardAction(threadDump, project));
        toolbarActions.add(new SortThreadsAction());
        //toolbarActions.add(new ShowRecentlyChanged());
        add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).getComponent(), BorderLayout.WEST);

        final Splitter splitter = new Splitter(false, 0.3f);
        splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myThreadList, SideBorder.LEFT | SideBorder.RIGHT));
        splitter.setSecondComponent(consoleView.getComponent());

        add(splitter, BorderLayout.CENTER);

        new ListSpeedSearch(myThreadList).setComparator(new SpeedSearchComparator(false, true));
    }

    private static Image getThreadStateIcon(final ThreadState threadState) {
        Image icon = getThreadIcon(threadState);
        final boolean daemon = threadState.isDaemon();
        if (!daemon) {
            return icon;
        }
        return ImageEffects.layered(icon, PlatformIconGroup.redbadge());
    }

    private static Image getThreadIcon(ThreadState threadState) {
        if (threadState.isSleeping()) {
            return PlatformIconGroup.actionsPause();
        }
        if (threadState.isWaiting()) {
            return PlatformIconGroup.vcsHistory();
        }
        if (threadState.getOperation() == ThreadOperation.Socket) {
            return PlatformIconGroup.nodesPlugin();
        }
        if (threadState.getOperation() == ThreadOperation.IO) {
            return PlatformIconGroup.actionsMenu_saveall();
        }
        if (threadState.isEDT()) {
            if ("idle".equals(threadState.getThreadStateDetail())) {
                return PlatformIconGroup.runconfigurationsTestpassed();
            }
            return PlatformIconGroup.actionsProfilecpu();
        }
        return PlatformIconGroup.actionsResume();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    @Override
    public Component getUIComponent() {
        throw new UnsupportedOperationException("desktop only");
    }

    private static enum StateCode {
        RUN,
        RUN_IO,
        RUN_SOCKET,
        PAUSED,
        LOCKED,
        EDT,
        IDLE
    }

    private static StateCode getThreadStateCode(final ThreadState state) {
        if (state.isSleeping()) {
            return StateCode.PAUSED;
        }
        if (state.isWaiting()) {
            return StateCode.LOCKED;
        }
        if (state.getOperation() == ThreadOperation.Socket) {
            return StateCode.RUN_SOCKET;
        }
        if (state.getOperation() == ThreadOperation.IO) {
            return StateCode.RUN_IO;
        }
        if (state.isEDT()) {
            return "idle".equals(state.getThreadStateDetail()) ? StateCode.IDLE : StateCode.EDT;
        }
        return StateCode.RUN;
    }

    private static SimpleTextAttributes getAttributes(final ThreadState threadState) {
        if (threadState.isSleeping()) {
            return SimpleTextAttributes.GRAY_ATTRIBUTES;
        }
        if (threadState.isEmptyStackTrace() || threadState.isBuiltinThread()) {
            return new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.GRAY.brighter());
        }
        if (threadState.isEDT()) {
            return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        }
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    private static class ThreadListCellRenderer extends ColoredListCellRenderer<ThreadState> {

        @Override
        protected void customizeCellRenderer(final JList list, final ThreadState threadState, final int index, final boolean selected, final boolean hasFocus) {
            setIcon(getThreadStateIcon(threadState));
            if (!selected) {
                ThreadState selectedThread = (ThreadState) list.getSelectedValue();
                if (threadState.isDeadlocked()) {
                    setBackground(LightColors.RED);
                }
                else if (selectedThread != null && threadState.isAwaitedBy(selectedThread)) {
                    setBackground(JBColor.YELLOW);
                }
                else {
                    setBackground(UIUtil.getListBackground());
                }
            }
            SimpleTextAttributes attrs = getAttributes(threadState);
            append(threadState.getName() + " (", attrs);
            String detail = threadState.getThreadStateDetail();
            if (detail == null) {
                detail = threadState.getState();
            }
            if (detail.length() > 30) {
                detail = detail.substring(0, 30) + "...";
            }
            append(detail, attrs);
            append(")", attrs);
            if (threadState.getExtraState() != null) {
                append(" [" + threadState.getExtraState() + "]", attrs);
            }
        }
    }

    @Override
    public void selectStackFrame(int index) {
        myThreadList.setSelectedIndex(index);
    }

    private class SortThreadsAction extends DumbAwareAction {
        private final Comparator<ThreadState> BY_TYPE = (o1, o2) -> {
            final int s1 = getThreadStateCode(o1).ordinal();
            final int s2 = getThreadStateCode(o2).ordinal();
            if (s1 == s2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
            else {
                return s1 < s2 ? -1 : 1;
            }
        };

        private final Comparator<ThreadState> BY_NAME = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
        private Comparator<ThreadState> COMPARATOR = BY_TYPE;
        private static final String TYPE_LABEL = "Sort threads by type";
        private static final String NAME_LABEL = "Sort threads by name";

        public SortThreadsAction() {
            super(TYPE_LABEL);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            final DefaultListModel model = (DefaultListModel) myThreadList.getModel();
            final ThreadState selected = (ThreadState) myThreadList.getSelectedValue();
            ArrayList<ThreadState> states = new ArrayList<ThreadState>();
            for (int i = 0; i < model.getSize(); i++) {
                states.add((ThreadState) model.getElementAt(i));
            }
            Collections.sort(states, COMPARATOR);
            int selectedIndex = 0;
            for (int i = 0; i < states.size(); i++) {
                final ThreadState state = states.get(i);
                model.setElementAt(state, i);
                if (state == selected) {
                    selectedIndex = i;
                }
            }
            myThreadList.setSelectedIndex(selectedIndex);
            COMPARATOR = COMPARATOR == BY_TYPE ? BY_NAME : BY_TYPE;
            update(e);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setIcon(COMPARATOR == BY_TYPE ? AllIcons.ObjectBrowser.SortByType : AllIcons.ObjectBrowser.Sorted);
            e.getPresentation().setText(COMPARATOR == BY_TYPE ? TYPE_LABEL : NAME_LABEL);
        }
    }

    private static class CopyToClipboardAction extends DumbAwareAction {
        private final List<ThreadState> myThreadDump;
        private final Project myProject;

        public CopyToClipboardAction(List<ThreadState> threadDump, Project project) {
            super("Copy to Clipboard", "Copy whole thread dump to clipboard", AllIcons.Actions.Copy);
            myThreadDump = threadDump;
            myProject = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Full thread dump").append("\n\n");
            for (ThreadState state : myThreadDump) {
                buf.append(state.getStackTrace()).append("\n\n");
            }
            CopyPasteManager.getInstance().setContents(new StringSelection(buf.toString()));

            ExecutionNotificationGroupHolder.ANALYZE_THREAD_DUMP
                .createNotification("Full thread dump was successfully copied to clipboard", NotificationType.INFORMATION)
                .notify(myProject);
        }
    }
}
