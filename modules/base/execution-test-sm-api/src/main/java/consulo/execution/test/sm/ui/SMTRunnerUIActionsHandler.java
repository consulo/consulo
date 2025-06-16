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

import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.ProxyFilters;
import consulo.execution.test.sm.runner.SMTestProxy;

import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerUIActionsHandler extends TestResultsViewer.SMEventsAdapter {
    private final TestConsoleProperties myConsoleProperties;

    public SMTRunnerUIActionsHandler(TestConsoleProperties consoleProperties) {
        myConsoleProperties = consoleProperties;
    }

    @Override
    public void onTestingFinished(TestResultsViewer sender) {
        // select first defect at the end (my be TRACK_RUNNING_TEST was enabled and affects on the fly selection)
        SMTestProxy testsRootNode = sender.getTestsRootNode();
        if (TestConsoleProperties.SELECT_FIRST_DEFECT.value(myConsoleProperties)) {
            AbstractTestProxy firstDefect;

            // defects priority:
            // ERROR -> FAILURE -> GENERAL DEFECTIVE NODE
            List<SMTestProxy> allTests = testsRootNode.getAllTests();
            AbstractTestProxy firstError = ProxyFilters.ERROR_LEAF.detectIn(allTests);
            if (firstError != null) {
                firstDefect = firstError;
            }
            else {
                AbstractTestProxy firstFailure = ProxyFilters.FAILURE_LEAF.detectIn(allTests);
                if (firstFailure != null) {
                    firstDefect = firstFailure;
                }
                else {
                    firstDefect = null;
                }
            }

            // select if detected
            if (firstDefect != null) {
                sender.selectAndNotify(firstDefect);
            }
        }
    }
}
