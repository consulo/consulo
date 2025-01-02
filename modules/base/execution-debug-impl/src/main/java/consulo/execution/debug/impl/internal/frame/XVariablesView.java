/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author nik
 */
public class XVariablesView extends XVariablesViewBase implements DataProvider {
    public static final Key<InlineVariablesInfo> DEBUG_VARIABLES = Key.create("debug.variables");
    public static final Key<Object2LongMap<VirtualFile>> DEBUG_VARIABLES_TIMESTAMPS = Key.create("debug.variables.timestamps");
    private final JPanel myComponent;
    @Nonnull
    protected final XDebugSessionImpl mySession;

    public XVariablesView(@Nonnull XDebugSessionImpl session) {
        super(session.getProject(), session.getDebugProcess().getEditorsProvider(), session.getValueMarkers());
        mySession = session;
        myComponent = new BorderLayoutPanel();
        myComponent.add(super.getPanel());
        DataManager.registerDataProvider(myComponent, this);

        JComponent topPanel = createTopPanel();
        if (topPanel != null) {
            myComponent.add(topPanel, BorderLayout.NORTH);
        }
    }

    @Nonnull
    public XDebugSessionImpl getSession() {
        return mySession;
    }

    protected void beforeTreeBuild(@Nonnull SessionEvent event) {
    }

    @Nullable
    protected JComponent createTopPanel() {
        return null;
    }

    @Override
    public JPanel getPanel() {
        return myComponent;
    }

    @Override
    public void processSessionEvent(@Nonnull SessionEvent event, @Nonnull XDebugSession session) {
        if (ApplicationManager.getApplication().isDispatchThread()) { // mark nodes obsolete asap
            getTree().markNodesObsolete();
        }

        XStackFrame stackFrame = session.getCurrentStackFrame();
        DebuggerUIImplUtil.invokeLater(() -> {
            XDebuggerTree tree = getTree();

            if (event == SessionEvent.BEFORE_RESUME || event == SessionEvent.SETTINGS_CHANGED) {
                saveCurrentTreeState(stackFrame);
                if (event == SessionEvent.BEFORE_RESUME) {
                    return;
                }
            }

            tree.markNodesObsolete();
            if (stackFrame != null) {
                cancelClear();
                beforeTreeBuild(event);
                buildTreeAndRestoreState(stackFrame);
            }
            else {
                requestClear();
            }
        });
    }

    @Override
    public void dispose() {
        clearInlineData(getTree());
        super.dispose();
    }

    private static void clearInlineData(XDebuggerTree tree) {
        tree.getProject().putUserData(DEBUG_VARIABLES, null);
        tree.getProject().putUserData(DEBUG_VARIABLES_TIMESTAMPS, null);
        tree.updateEditor();
        XVariablesViewBase.clearInlays(tree);
    }

    protected void addEmptyMessage(XValueContainerNode root) {
        XDebugSession session = XDebugView.getSession(getPanel());
        if (session != null) {
            if (!session.isStopped() && session.isPaused()) {
                root.setInfoMessage("Frame is not available", null);
            }
            else {
                XDebugProcess debugProcess = session.getDebugProcess();
                root.setInfoMessage(debugProcess.getCurrentStateMessage(), debugProcess.getCurrentStateHyperlinkListener());
            }
        }
    }

    @Override
    protected void clear() {
        XDebuggerTree tree = getTree();
        tree.setSourcePosition(null);
        clearInlineData(tree);

        XValueContainerNode root = createNewRootNode(null);
        addEmptyMessage(root);
        super.clear();
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        return VirtualFile.KEY == dataId ? getCurrentFile(getTree()) : null;
    }

    public static class InlineVariablesInfo {
        private final Map<Pair<VirtualFile, Integer>, Set<Entry>> myData = new HashMap<>();
        private final Object2LongMap<VirtualFile> myTimestamps = new Object2LongOpenHashMap<>();

        @Nullable
        public synchronized List<XValueNodeImpl> get(@Nonnull VirtualFile file, int line, long currentTimestamp) {
            long timestamp = myTimestamps.getOrDefault(file, -1);
            if (timestamp == -1 || timestamp < currentTimestamp) {
                return null;
            }
            Set<Entry> entries = myData.get(Pair.create(file, line));
            if (entries == null) {
                return null;
            }
            return ContainerUtil.map(entries, entry -> entry.myNode);
        }

        public synchronized void put(@Nonnull VirtualFile file, @Nonnull XSourcePosition position, @Nonnull XValueNodeImpl node, long timestamp) {
            myTimestamps.put(file, timestamp);
            Pair<VirtualFile, Integer> key = Pair.create(file, position.getLine());
            myData.computeIfAbsent(key, k -> new TreeSet<>()).add(new Entry(position.getOffset(), node));
        }

        private static class Entry implements Comparable<Entry> {
            private final long myOffset;
            private final XValueNodeImpl myNode;

            public Entry(long offset, @Nonnull XValueNodeImpl node) {
                myOffset = offset;
                myNode = node;
            }

            @Override
            public int compareTo(Entry o) {
                if (myNode == o.myNode) {
                    return 0;
                }
                int res = Comparing.compare(myOffset, o.myOffset);
                if (res == 0) {
                    return XValueNodeImpl.COMPARATOR.compare(myNode, o.myNode);
                }
                return res;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                Entry entry = (Entry) o;
                return myNode.equals(entry.myNode);
            }

            @Override
            public int hashCode() {
                return myNode.hashCode();
            }
        }
    }
}
