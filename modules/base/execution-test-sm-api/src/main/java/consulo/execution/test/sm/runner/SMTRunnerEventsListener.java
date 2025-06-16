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
package consulo.execution.test.sm.runner;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Roman Chernyatchik
 * <p>
 * Handles Test Runner events
 */
@TopicAPI(ComponentScope.PROJECT)
public interface SMTRunnerEventsListener {
    /**
     * On start testing, before tests and suits launching
     *
     * @param testsRoot
     */
    void onTestingStarted(@Nonnull SMTestProxy.SMRootTestProxy testsRoot);

    /**
     * After test framework finish testing
     *
     * @param testsRootNode
     */
    void onTestingFinished(@Nonnull SMTestProxy.SMRootTestProxy testsRoot);

    /*
     * Tests count in next suite. For several suites this method will be invoked several times
     */
    void onTestsCountInSuite(int count);

    void onTestStarted(@Nonnull SMTestProxy test);

    void onTestFinished(@Nonnull SMTestProxy test);

    void onTestFailed(@Nonnull SMTestProxy test);

    void onTestIgnored(@Nonnull SMTestProxy test);

    void onSuiteFinished(@Nonnull SMTestProxy suite);

    void onSuiteStarted(@Nonnull SMTestProxy suite);

    // Custom progress statistics

    /**
     * @param categoryName If isn't empty then progress statistics will use only custom start/failed events.
     *                     If name is empty string statistics will be switched to normal mode
     * @param testCount    - 0 will be considered as unknown tests number
     */
    void onCustomProgressTestsCategory(@Nullable final String categoryName, final int testCount);

    void onCustomProgressTestStarted();

    void onCustomProgressTestFailed();

    void onCustomProgressTestFinished();

    void onSuiteTreeNodeAdded(SMTestProxy testProxy);

    void onSuiteTreeStarted(SMTestProxy suite);

}
