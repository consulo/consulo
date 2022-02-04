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

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestFrameworkRunningModel;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import consulo.disposer.Disposable;
import javax.annotation.Nonnull;

/**
 * @author Roman Chernyatchik
 */
public interface TestResultsViewer extends Disposable {
  /**
   * Fake Root for toplevel test suits/tests
   *
   * @return root
   */
  SMTestProxy getTestsRootNode();

  /**
   * Selects test or suite in Tests tree and notify about selection changed
   *
   * @param proxy
   */
  void selectAndNotify(@javax.annotation.Nullable AbstractTestProxy proxy);

  void addEventsListener(EventsListener listener);

  void setShowStatisticForProxyHandler(PropagateSelectionHandler handler);

  /**
   * If handler for statistics was set this method will execute it
   */
  void showStatisticsForSelectedProxy();

  interface EventsListener extends TestProxyTreeSelectionListener {
    void onTestingStarted(TestResultsViewer sender);

    void onTestingFinished(TestResultsViewer sender);

    void onTestNodeAdded(TestResultsViewer sender, SMTestProxy test);
  }

  class SMEventsAdapter implements EventsListener {

    @Override
    public void onTestingStarted(TestResultsViewer sender) {
    }

    @Override
    public void onTestingFinished(TestResultsViewer sender) {
    }

    @Override
    public void onTestNodeAdded(TestResultsViewer sender, SMTestProxy test) {
    }

    @Override
    public void onSelected(@javax.annotation.Nullable SMTestProxy selectedTestProxy,
                           @Nonnull TestResultsViewer viewer,
                           @Nonnull TestFrameworkRunningModel model) {
    }
  }
}
