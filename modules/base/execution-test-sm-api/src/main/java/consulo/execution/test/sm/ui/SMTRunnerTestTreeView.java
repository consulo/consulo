/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.test.sm.ui;

import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.ui.TestTreeView;
import consulo.dataContext.DataSink;
import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerTestTreeView extends TestTreeView {
    public static final Key<SMTRunnerTestTreeView> SM_TEST_RUNNER_VIEW = Key.create("SM_TEST_RUNNER_VIEW");

    private @Nullable TestResultsViewer myResultsViewer;

    @Override
    protected TreeCellRenderer getRenderer(TestConsoleProperties properties) {
        return new TestTreeRenderer(properties);
    }

    @Nullable
    @Override
    public SMTestProxy getSelectedTest(TreePath selectionPath) {
        Object lastComponent = selectionPath.getLastPathComponent();
        assert lastComponent != null;

        return getTestProxyFor(lastComponent);
    }

    public static @Nullable SMTestProxy getTestProxyFor(Object treeNode) {
        Object userObj = ((DefaultMutableTreeNode) treeNode).getUserObject();
        if (userObj instanceof SMTRunnerNodeDescriptor runnerNodeDescriptor) {
            return runnerNodeDescriptor.getElement();
        }

        return null;
    }

    public void setTestResultsViewer(TestResultsViewer resultsViewer) {
        myResultsViewer = resultsViewer;
    }

    public @Nullable TestResultsViewer getResultsViewer() {
        return myResultsViewer;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(SM_TEST_RUNNER_VIEW, this);
    }
}
