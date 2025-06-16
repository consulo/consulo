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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerEventsAdapter implements SMTRunnerEventsListener {
    @Override
    public void onTestingStarted(@Nonnull SMTestProxy.SMRootTestProxy testsRoot) {
    }

    @Override
    public void onTestingFinished(@Nonnull SMTestProxy.SMRootTestProxy testsRoot) {
    }

    @Override
    public void onTestsCountInSuite(int count) {
    }

    @Override
    public void onTestStarted(@Nonnull SMTestProxy test) {
    }

    @Override
    public void onTestFinished(@Nonnull SMTestProxy test) {
    }

    @Override
    public void onTestFailed(@Nonnull SMTestProxy test) {
    }

    @Override
    public void onTestIgnored(@Nonnull SMTestProxy test) {
    }

    @Override
    public void onSuiteStarted(@Nonnull SMTestProxy suite) {
    }

    @Override
    public void onSuiteFinished(@Nonnull SMTestProxy suite) {
    }

    // Custom progress status

    @Override
    public void onCustomProgressTestsCategory(@Nullable String categoryName, int testCount) {
    }

    @Override
    public void onCustomProgressTestStarted() {
    }

    @Override
    public void onCustomProgressTestFailed() {
    }

    @Override
    public void onCustomProgressTestFinished() {
    }

    @Override
    public void onSuiteTreeNodeAdded(SMTestProxy testProxy) {
    }

    @Override
    public void onSuiteTreeStarted(SMTestProxy suite) {
    }
}
