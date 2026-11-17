/*
 * Copyright 2013-2026 consulo.io
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

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreePanel;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeState;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.awt.util.SingleAlarm;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The live threads of the debug process with their stack frames, see
 * {@link HasRunningExecutionStacks#computeRunningExecutionStacks}.
 */
public class XThreadsView extends XDebugView {
    private static final int REBUILD_DELAY = 200;

    private final XDebugSession mySession;
    private final XDebuggerTreePanel myTreePanel;
    private final JPanel myPanel;
    private final SingleAlarm myRebuildAlarm;
    private boolean myWasShowing;

    public XThreadsView(Project project, XDebugSession session) {
        mySession = session;
        myTreePanel = new XDebuggerTreePanel(
            project,
            session.getDebugProcess().getEditorsProvider(),
            this,
            null,
            XDebuggerActions.THREADS_VIEW_POPUP,
            null
        );
        myPanel = new SelectedStacksPanel();
        myPanel.add(myTreePanel.getMainPanel(), BorderLayout.CENTER);

        getTree().getEmptyText().setText(XDebuggerLocalize.debuggerThreadsNotAvailable());

        new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return true;
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
            }

            @Override
            protected boolean needToCheckFocus() {
                return false;
            }

            @Override
            protected void scrollToSource(Component tree) {
                TreePath path = tree instanceof XDebuggerTree debuggerTree ? debuggerTree.getSelectionPath() : null;
                if (path != null
                    && path.getLastPathComponent() instanceof XValueNodeImpl node
                    && node.getValueContainer() instanceof FrameValue frameValue) {
                    mySession.setCurrentStackFrame(frameValue.getExecutionStack(), frameValue.getFrame(), false);
                }
            }
        }.install(getTree());

        myRebuildAlarm = new SingleAlarm(this::rebuild, REBUILD_DELAY, this);

        myPanel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0) {
                boolean isCurrentlyShowing = myPanel.isShowing();
                if (!myWasShowing && isCurrentlyShowing) {
                    requestRebuild();
                }
                myWasShowing = isCurrentlyShowing;
            }
        });
    }

    public XDebuggerTree getTree() {
        return myTreePanel.getTree();
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public XDebuggerTree getDefaultFocusedComponent() {
        return getTree();
    }

    @Override
    protected XDebugSession getSession() {
        return mySession;
    }

    private void requestRebuild() {
        myRebuildAlarm.cancelAndRequest();
    }

    private void rebuild() {
        if (myPanel.isShowing()) {
            XDebuggerTree tree = getTree();
            XDebuggerTreeState state = XDebuggerTreeState.saveState(tree);
            tree.setRoot(new XThreadsRootNode(tree, mySession), false);
            state.restoreState(tree);
        }
    }

    @Override
    protected void clear() {
        DebuggerUIImplUtil.invokeLater(() -> {
            XDebuggerTree tree = getTree();
            tree.setRoot(new XValueContainerNode<XValueContainer>(tree, null, new XValueContainer() {
            }) {
            }, false);
        });
    }

    @Override
    public void processSessionEvent(SessionEvent event, XDebugSession session) {
        if (event == SessionEvent.BEFORE_RESUME) {
            return;
        }
        // Do not refresh a tree on a FRAME_CHANGED event
        // so that selecting stack frames does not collapse a thread node.
        if (event == SessionEvent.FRAME_CHANGED) {
            return;
        }
        requestRebuild();
    }

    @Override
    public void dispose() {
    }

    /**
     * The execution stacks of the selected thread nodes, for the actions of the threads.
     */
    public static List<XExecutionStack> getSelectedStacks(XDebuggerTree tree) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return List.of();
        }
        List<XExecutionStack> stacks = new ArrayList<>();
        for (TreePath path : paths) {
            if (path.getLastPathComponent() instanceof XValueNodeImpl node && node.getValueContainer() instanceof FramesContainer frames) {
                stacks.add(frames.getExecutionStack());
            }
        }
        return stacks;
    }

    private class SelectedStacksPanel extends JPanel implements UiDataProvider {
        private SelectedStacksPanel() {
            super(new BorderLayout());
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            List<XExecutionStack> stacks = getSelectedStacks(getTree());
            if (!stacks.isEmpty()) {
                sink.set(XExecutionStack.SELECTED_STACKS, stacks);
            }
        }
    }

    public static class ThreadsContainer extends XValueContainer {
        private final XDebugSession mySession;

        public ThreadsContainer(XDebugSession session) {
            mySession = session;
        }

        @Override
        public void computeChildren(XCompositeNode node) {
            XSuspendContext.XExecutionStackGroupContainer container = new XSuspendContext.XExecutionStackGroupContainer() {
                @Override
                public void errorOccurred(LocalizeValue errorMessage) {
                    setMessage(node, errorMessage);
                }

                @Override
                public void addExecutionStack(List<? extends XExecutionStack> executionStacks, boolean last) {
                    XValueChildrenList children = new XValueChildrenList();
                    for (XExecutionStack stack : executionStacks) {
                        children.add(stack.getDisplayName(), new FramesContainer(stack, mySession));
                    }
                    node.addChildren(children, last);
                }

                @Override
                public boolean isObsolete() {
                    return node.isObsolete();
                }

                @Override
                public void addExecutionStackGroups(List<? extends XExecutionStackGroup> executionStackGroups, boolean last) {
                    XValueChildrenList children = new XValueChildrenList();
                    for (XExecutionStackGroup group : executionStackGroups) {
                        children.add(group.getName(), new ThreadGroupContainer(group, mySession));
                    }
                    node.addChildren(children, last);
                }
            };
            if (mySession.getDebugProcess() instanceof HasRunningExecutionStacks hasRunningExecutionStacks) {
                hasRunningExecutionStacks.computeRunningExecutionStacks(container, mySession.getSuspendContext());
            }
            else {
                container.addExecutionStack(List.of(), true);
            }
        }
    }

    public static class ThreadGroupContainer extends XValue {
        private final XExecutionStackGroup myGroup;
        private final XDebugSession mySession;

        public ThreadGroupContainer(XExecutionStackGroup group, XDebugSession session) {
            myGroup = group;
            mySession = session;
        }

        public XExecutionStackGroup getGroup() {
            return myGroup;
        }

        @Override
        public void computeChildren(XCompositeNode node) {
            XValueChildrenList children = new XValueChildrenList();
            for (XExecutionStackGroup group : myGroup.getGroups()) {
                children.add(group.getName(), new ThreadGroupContainer(group, mySession));
            }
            for (XExecutionStack stack : myGroup.getStacks()) {
                children.add(stack.getDisplayName(), new FramesContainer(stack, mySession));
            }
            node.addChildren(children, true);
        }

        @Override
        public void computePresentation(XValueNode node, XValuePlace place) {
            setEmptyValuePresentation(node, myGroup.getIcon(), null);
        }
    }

    public static class FramesContainer extends XValue {
        private final XExecutionStack myExecutionStack;
        private final XDebugSession mySession;

        public FramesContainer(XExecutionStack executionStack, XDebugSession session) {
            myExecutionStack = executionStack;
            mySession = session;
        }

        public XExecutionStack getExecutionStack() {
            return myExecutionStack;
        }

        @Override
        public void computeChildren(XCompositeNode node) {
            if (mySession.getSuspendContext() == null) {
                setMessage(node, XDebuggerLocalize.debuggerFramesNotAvailable());
                return;
            }

            myExecutionStack.computeStackFrames(new XExecutionStack.XStackFrameContainer() {
                @Override
                public void errorOccurred(LocalizeValue errorMessage) {
                    setMessage(node, errorMessage);
                }

                @Override
                public void addStackFrames(List<? extends XStackFrame> stackFrames, boolean last) {
                    XValueChildrenList children = new XValueChildrenList();
                    for (XStackFrame frame : stackFrames) {
                        children.add("", new FrameValue(myExecutionStack, frame));
                    }
                    node.addChildren(children, last);
                }

                @Override
                public boolean isObsolete() {
                    return node.isObsolete();
                }
            });
        }

        @Override
        public void computePresentation(XValueNode node, XValuePlace place) {
            XExecutionStack.AdditionalDisplayInfo info = myExecutionStack.getAdditionalDisplayInfo();
            setEmptyValuePresentation(node, myExecutionStack.getIcon(), info == null ? null : info.getText().get());
        }
    }

    public static class FrameValue extends XValue {
        private final XExecutionStack myExecutionStack;
        private final XStackFrame myFrame;

        public FrameValue(XExecutionStack executionStack, XStackFrame frame) {
            myExecutionStack = executionStack;
            myFrame = frame;
        }

        public XExecutionStack getExecutionStack() {
            return myExecutionStack;
        }

        public XStackFrame getFrame() {
            return myFrame;
        }

        @Override
        public void computePresentation(XValueNode node, XValuePlace place) {
            FramePresentation presentation = new FramePresentation();
            myFrame.customizePresentation(presentation);
            node.setPresentation(presentation.myIcon, new XValuePresentation() {
                @Override
                public String getSeparator() {
                    return "";
                }

                @Override
                public void renderValue(XValueTextRenderer renderer) {
                    List<String> texts = presentation.getTexts();
                    List<SimpleTextAttributes> allAttributes = presentation.getAttributes();
                    for (int i = 0; i < texts.size(); i++) {
                        String text = texts.get(i);
                        SimpleTextAttributes attributes = allAttributes.get(i);
                        if (attributes == SimpleTextAttributes.GRAYED_ATTRIBUTES
                            || attributes == SimpleTextAttributes.GRAY_ATTRIBUTES
                            || attributes == SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES) {
                            renderer.renderComment(text);
                        }
                        else if (attributes == SimpleTextAttributes.ERROR_ATTRIBUTES) {
                            renderer.renderError(LocalizeValue.of(text));
                        }
                        else {
                            renderer.renderValue(text);
                        }
                    }
                }
            }, false);
        }
    }

    /**
     * The presentation of a stack frame as {@link XStackFrame#customizePresentation(ColoredTextContainer)} fills it.
     */
    private static class FramePresentation extends SimpleColoredText {
        private @Nullable Image myIcon;

        @Override
        public void setIcon(@Nullable Image image) {
            myIcon = image;
        }
    }

    public static class XThreadsRootNode extends XValueContainerNode<ThreadsContainer> {
        public XThreadsRootNode(XDebuggerTree tree, XDebugSession session) {
            super(tree, null, new ThreadsContainer(session));
            setLeaf(false);
        }
    }

    private static void setMessage(XCompositeNode node, LocalizeValue text) {
        // remove temporary loading message nodes
        node.addChildren(XValueChildrenList.EMPTY, true);
        node.setMessage(text, null, SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
    }

    private static void setEmptyValuePresentation(XValueNode node, @Nullable Image icon, @Nullable String comment) {
        node.setPresentation(icon, new XValuePresentation() {
            @Override
            public String getSeparator() {
                return "";
            }

            @Override
            public void renderValue(XValueTextRenderer renderer) {
                if (comment != null) {
                    renderer.renderComment(comment);
                }
            }
        }, true);
    }
}
