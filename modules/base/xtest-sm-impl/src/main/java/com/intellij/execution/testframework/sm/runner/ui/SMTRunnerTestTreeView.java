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
package com.intellij.execution.testframework.sm.runner.ui;

import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.TestTreeView;
import com.intellij.execution.testframework.sm.runner.SMTRunnerNodeDescriptor;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * @author: Roman Chernyatchik
 */
public class SMTRunnerTestTreeView extends TestTreeView {

  public static final Key<SMTRunnerTestTreeView> SM_TEST_RUNNER_VIEW = Key.create("SM_TEST_RUNNER_VIEW");

  @javax.annotation.Nullable
  private TestResultsViewer myResultsViewer;

  protected TreeCellRenderer getRenderer(final TestConsoleProperties properties) {
    return new TestTreeRenderer(properties);
  }

  @Nullable
  public SMTestProxy getSelectedTest(@Nonnull final TreePath selectionPath) {
    final Object lastComponent = selectionPath.getLastPathComponent();
    assert lastComponent != null;

    return getTestProxyFor(lastComponent);
  }

  @Nullable
  public static SMTestProxy getTestProxyFor(final Object treeNode) {
    final Object userObj = ((DefaultMutableTreeNode)treeNode).getUserObject();
    if (userObj instanceof SMTRunnerNodeDescriptor) {
      return ((SMTRunnerNodeDescriptor)userObj).getElement();
    }

    return null;
  }

  public void setTestResultsViewer(final TestResultsViewer resultsViewer) {
    myResultsViewer = resultsViewer;
  }

  @javax.annotation.Nullable
  public TestResultsViewer getResultsViewer() {
    return myResultsViewer;
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (SM_TEST_RUNNER_VIEW == dataId) {
      return this;
    }
    return super.getData(dataId);
  }
}
