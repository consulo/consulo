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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.execution.debug.frame.HeadlessValueEvaluationCallback;
import consulo.execution.debug.frame.XFullValueEvaluator;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNodeImpl;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class XFetchValueActionBase extends AnAction {
    protected XFetchValueActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        for (XValueNodeImpl node : XDebuggerTreeActionBase.getSelectedNodes(e.getDataContext())) {
            if (isEnabled(e, node)) {
                return;
            }
        }
        e.getPresentation().setEnabled(false);
    }

    protected boolean isEnabled(@Nonnull AnActionEvent event, @Nonnull XValueNodeImpl node) {
        if (node instanceof WatchNodeImpl || node.isComputed()) {
            event.getPresentation().setEnabled(true);
            return true;
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        List<XValueNodeImpl> nodes = XDebuggerTreeActionBase.getSelectedNodes(e.getDataContext());
        if (nodes.isEmpty()) {
            return;
        }

        ValueCollector valueCollector = createCollector(e);
        for (XValueNodeImpl node : nodes) {
            addToCollector(nodes, node, valueCollector);
        }
        valueCollector.processed = true;
        valueCollector.finish();
    }

    protected void addToCollector(
        @Nonnull List<XValueNodeImpl> paths,
        @Nonnull XValueNodeImpl valueNode,
        @Nonnull ValueCollector valueCollector
    ) {
        if (paths.size() > 1) { // multiselection - copy the whole node text, see IDEA-136722
            valueCollector.add(valueNode.getText().toString(), valueNode.getPath().getPathCount());
        }
        else {
            XFullValueEvaluator fullValueEvaluator = valueNode.getFullValueEvaluator();
            if (fullValueEvaluator == null || !fullValueEvaluator.isShowValuePopup()) {
                valueCollector.add(StringUtil.notNullize(DebuggerUIImplUtil.getNodeRawValue(valueNode)));
            }
            else {
                new CopyValueEvaluationCallback(valueNode, valueCollector).startFetchingValue(fullValueEvaluator);
            }
        }
    }

    @Nonnull
    protected ValueCollector createCollector(@Nonnull AnActionEvent e) {
        return new ValueCollector(XDebuggerTree.getTree(e.getDataContext()));
    }

    public class ValueCollector {
        private final List<String> values = new SmartList<>();
        private final Int2IntMap indents = new Int2IntOpenHashMap();
        private final XDebuggerTree myTree;
        private volatile boolean processed;

        public ValueCollector(XDebuggerTree tree) {
            myTree = tree;
        }

        public void add(@Nonnull String value) {
            values.add(value);
        }

        public void add(@Nonnull String value, int indent) {
            values.add(value);
            indents.put(values.size() - 1, indent);
        }

        public void finish() {
            Project project = myTree.getProject();
            if (processed && !values.contains(null) && !project.isDisposed()) {
                int minIndent = Integer.MAX_VALUE;
                for (Integer indent : indents.values()) {
                    minIndent = Math.min(minIndent, indent);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        sb.append("\n");
                    }
                    int indent = indents.get(i);
                    if (indent > 0) {
                        StringUtil.repeatSymbol(sb, ' ', indent - minIndent);
                    }
                    sb.append(values.get(i));
                }
                handleInCollector(project, sb.toString(), myTree);
            }
        }

        public void handleInCollector(Project project, String value, XDebuggerTree tree) {
            handle(project, value, tree);
        }

        public int acquire() {
            int index = values.size();
            values.add(null);
            return index;
        }

        public void evaluationComplete(int index, @Nonnull String value) {
            AppUIUtil.invokeOnEdt(() -> {
                values.set(index, value);
                finish();
            });
        }
    }

    protected abstract void handle(Project project, String value, XDebuggerTree tree);

    private static final class CopyValueEvaluationCallback extends HeadlessValueEvaluationCallback {
        private final int myValueIndex;
        private final ValueCollector myValueCollector;

        public CopyValueEvaluationCallback(@Nonnull XValueNodeImpl node, @Nonnull ValueCollector valueCollector) {
            super(node, node.getTree().getProject());

            myValueCollector = valueCollector;
            myValueIndex = valueCollector.acquire();
        }

        @Override
        protected void evaluationComplete(@Nonnull LocalizeValue value, @Nonnull Project project) {
            myValueCollector.evaluationComplete(myValueIndex, value.get());
        }
    }
}
