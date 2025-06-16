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
package consulo.execution.test.sm.runner;

import consulo.application.Application;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.test.sm.runner.event.*;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Function;

/**
 * This class fires events to SMTRunnerEventsListener in event dispatch thread.
 *
 * @author Roman Chernyatchik
 */
public class GeneralToSMTRunnerEventsConvertor extends GeneralTestEventsProcessor {
    private final Map<String, SMTestProxy> myRunningTestsFullNameToProxy = new HashMap<>();
    private final TestSuiteStack mySuitesStack;
    private final Set<SMTestProxy> myCurrentChildren = new LinkedHashSet<>();
    private boolean myGetChildren = true;

    private boolean myIsTestingFinished;

    public GeneralToSMTRunnerEventsConvertor(
        Project project,
        @Nonnull SMTestProxy.SMRootTestProxy testsRootNode,
        @Nonnull String testFrameworkName
    ) {
        super(project, testFrameworkName, testsRootNode);
        mySuitesStack = new TestSuiteStack(testFrameworkName);
    }

    @Override
    @RequiredUIAccess
    protected SMTestProxy createProxy(String testName, String locationHint, String id, String parentNodeId) {
        SMTestProxy proxy = super.createProxy(testName, locationHint, id, parentNodeId);
        SMTestProxy currentSuite = getCurrentSuite();
        currentSuite.addChild(proxy);
        return proxy;
    }

    @Override
    @RequiredUIAccess
    protected SMTestProxy createSuite(String suiteName, String locationHint, String id, String parentNodeId) {
        SMTestProxy newSuite = super.createSuite(suiteName, locationHint, id, parentNodeId);
        SMTestProxy parentSuite = getCurrentSuite();

        parentSuite.addChild(newSuite);

        mySuitesStack.pushSuite(newSuite);

        return newSuite;
    }

    @Override
    public void onSuiteTreeEnded(String suiteName) {
        myBuildTreeRunnables.add(() -> mySuitesStack.popSuite(suiteName));
        super.onSuiteTreeEnded(suiteName);
    }

    @Override
    public void onStartTesting() {
        addToInvokeLater(() -> {
            mySuitesStack.pushSuite(myTestsRootProxy);
            myTestsRootProxy.setStarted();

            //fire
            fireOnTestingStarted(myTestsRootProxy);
        });
    }

    @Override
    public void onTestsReporterAttached() {
        addToInvokeLater(() -> fireOnTestsReporterAttached(myTestsRootProxy));
    }

    @Override
    public void onFinishTesting() {
        addToInvokeLater(() -> {
            if (myIsTestingFinished) {
                // has been already invoked!
                return;
            }
            myIsTestingFinished = true;

            // We don't know whether process was destroyed by user
            // or it finished after all tests have been run
            // Lets assume, if at finish all suites except root suite are passed
            // then all is ok otherwise process was terminated by user
            if (!isTreeComplete(myRunningTestsFullNameToProxy.keySet(), myTestsRootProxy)) {
                myTestsRootProxy.setTerminated();
                myRunningTestsFullNameToProxy.clear();
            }
            mySuitesStack.clear();
            myTestsRootProxy.setFinished();


            //fire events
            fireOnTestingFinished(myTestsRootProxy);
        });
        super.onFinishTesting();
    }

    @Override
    public void setPrinterProvider(@Nonnull TestProxyPrinterProvider printerProvider) {
    }

    @Override
    public void onTestStarted(@Nonnull TestStartedEvent testStartedEvent) {
        addToInvokeLater(() -> {
            String testName = testStartedEvent.getName();
            String locationUrl = testStartedEvent.getLocationUrl();
            boolean isConfig = testStartedEvent.isConfig();
            String fullName = getFullTestName(testName);

            if (myRunningTestsFullNameToProxy.containsKey(fullName)) {
                //Duplicated event
                logProblem("Test [" + fullName + "] has been already started");
                if (SMTestRunnerConnectionUtil.isInDebugMode()) {
                    return;
                }
            }

            SMTestProxy parentSuite = getCurrentSuite();
            SMTestProxy testProxy = locationUrl != null
                ? findChildByLocation(parentSuite, locationUrl, false)
                : findChildByName(parentSuite, fullName, false);
            if (testProxy == null) {
                // creates test
                testProxy = new SMTestProxy(testName, false, locationUrl, testStartedEvent.getMetainfo(), false);
                testProxy.setConfig(isConfig);
                if (myTreeBuildBeforeStart) {
                    testProxy.setTreeBuildBeforeStart();
                }

                if (myLocator != null) {
                    testProxy.setLocator(myLocator);
                }

                parentSuite.addChild(testProxy);

                if (myTreeBuildBeforeStart && myGetChildren) {
                    for (SMTestProxy proxy : parentSuite.getChildren()) {
                        if (!proxy.isFinal()) {
                            myCurrentChildren.add(proxy);
                        }
                    }
                    myGetChildren = false;
                }
            }

            // adds to running tests map
            myRunningTestsFullNameToProxy.put(fullName, testProxy);

            //Progress started
            testProxy.setStarted();

            //fire events
            fireOnTestStarted(testProxy);
        });
    }

    @Override
    public void onSuiteStarted(@Nonnull TestSuiteStartedEvent suiteStartedEvent) {
        addToInvokeLater(() -> {
            String suiteName = suiteStartedEvent.getName();
            String locationUrl = suiteStartedEvent.getLocationUrl();

            SMTestProxy parentSuite = getCurrentSuite();
            SMTestProxy newSuite = locationUrl != null
                ? findChildByLocation(parentSuite, locationUrl, true)
                : findChildByName(parentSuite, suiteName, true);
            if (newSuite == null) {
                //new suite
                newSuite =
                    new SMTestProxy(suiteName, true, locationUrl, suiteStartedEvent.getMetainfo(), parentSuite.isPreservePresentableName());
                if (myTreeBuildBeforeStart) {
                    newSuite.setTreeBuildBeforeStart();
                }

                if (myLocator != null) {
                    newSuite.setLocator(myLocator);
                }

                parentSuite.addChild(newSuite);
            }

            myGetChildren = true;
            mySuitesStack.pushSuite(newSuite);

            //Progress started
            newSuite.setSuiteStarted();

            //fire event
            fireOnSuiteStarted(newSuite);
        });
    }

    private SMTestProxy findChildByName(SMTestProxy parentSuite, String fullName, boolean preferSuite) {
        return findChild(parentSuite, fullName, SMTestProxy::getName, preferSuite);
    }

    private SMTestProxy findChildByLocation(SMTestProxy parentSuite, String fullName, boolean preferSuite) {
        return findChild(parentSuite, fullName, SMTestProxy::getLocationUrl, preferSuite);
    }

    private SMTestProxy findChild(
        SMTestProxy parentSuite,
        String fullName,
        Function<SMTestProxy, String> nameFunction,
        boolean preferSuite
    ) {
        if (myTreeBuildBeforeStart) {
            Set<SMTestProxy> acceptedProxies = new LinkedHashSet<>();
            Collection<? extends SMTestProxy> children = myGetChildren ? parentSuite.getChildren() : myCurrentChildren;
            for (SMTestProxy proxy : children) {
                if (fullName.equals(nameFunction.apply(proxy)) && !proxy.isFinal()) {
                    acceptedProxies.add(proxy);
                }
            }
            if (!acceptedProxies.isEmpty()) {
                return acceptedProxies.stream()
                    .filter(proxy -> proxy.isSuite() == preferSuite)
                    .findFirst()
                    .orElse(acceptedProxies.iterator().next());
            }
        }
        return null;
    }

    @Override
    public void onTestFinished(@Nonnull TestFinishedEvent testFinishedEvent) {
        addToInvokeLater(() -> {
            String testName = testFinishedEvent.getName();
            Long duration = testFinishedEvent.getDuration();
            String fullTestName = getFullTestName(testName);
            SMTestProxy testProxy = getProxyByFullTestName(fullTestName);

            if (testProxy == null) {
                logProblem("Test wasn't started! TestFinished event: name = {" + testName + "}. " +
                    cannotFindFullTestNameMsg(fullTestName));
                return;
            }

            testProxy.setDuration(duration != null ? duration : 0);
            testProxy.setFrameworkOutputFile(testFinishedEvent.getOutputFile());
            testProxy.setFinished();
            myRunningTestsFullNameToProxy.remove(fullTestName);
            myCurrentChildren.remove(testProxy);

            //fire events
            fireOnTestFinished(testProxy);
        });
    }

    @Override
    public void onSuiteFinished(@Nonnull TestSuiteFinishedEvent suiteFinishedEvent) {
        addToInvokeLater(() -> {
            String suiteName = suiteFinishedEvent.getName();
            SMTestProxy mySuite = mySuitesStack.popSuite(suiteName);
            if (mySuite != null) {
                mySuite.setFinished();
                myCurrentChildren.clear();
                myGetChildren = true;

                //fire events
                fireOnSuiteFinished(mySuite);
            }
        });
    }

    @Override
    public void onUncapturedOutput(@Nonnull String text, Key outputType) {
        addToInvokeLater(() -> {
            SMTestProxy currentProxy = findCurrentTestOrSuite();
            currentProxy.addOutput(text, outputType);
        });
    }

    @Override
    public void onError(
        @Nonnull String localizedMessage,
        @Nullable String stackTrace,
        boolean isCritical
    ) {
        addToInvokeLater(() -> {
            SMTestProxy currentProxy = findCurrentTestOrSuite();
            currentProxy.addError(localizedMessage, stackTrace, isCritical);
        });
    }


    @Override
    public void onTestFailure(@Nonnull TestFailedEvent testFailedEvent) {
        addToInvokeLater(() -> {
            String testName = testFailedEvent.getName();
            if (testName == null) {
                logProblem("No test name specified in " + testFailedEvent);
                return;
            }
            String localizedMessage = testFailedEvent.getLocalizedFailureMessage();
            String stackTrace = testFailedEvent.getStacktrace();
            boolean isTestError = testFailedEvent.isTestError();
            String comparisionFailureActualText = testFailedEvent.getComparisonFailureActualText();
            String comparisionFailureExpectedText = testFailedEvent.getComparisonFailureExpectedText();
            boolean inDebugMode = SMTestRunnerConnectionUtil.isInDebugMode();

            String fullTestName = getFullTestName(testName);
            SMTestProxy testProxy = getProxyByFullTestName(fullTestName);
            if (testProxy == null) {
                logProblem("Test wasn't started! TestFailure event: name = {" + testName + "}" +
                    ", message = {" + localizedMessage + "}" +
                    ", stackTrace = {" + stackTrace + "}. " +
                    cannotFindFullTestNameMsg(fullTestName));
                if (inDebugMode) {
                    return;
                }
                else {
                    // if hasn't been already reported
                    // 1. report
                    onTestStarted(new TestStartedEvent(testName, null));
                    // 2. add failure
                    testProxy = getProxyByFullTestName(fullTestName);
                }
            }

            if (testProxy == null) {
                return;
            }

            if (comparisionFailureActualText != null && comparisionFailureExpectedText != null) {
                testProxy.setTestComparisonFailed(
                    localizedMessage,
                    stackTrace,
                    comparisionFailureActualText,
                    comparisionFailureExpectedText,
                    testFailedEvent
                );
            }
            else if (comparisionFailureActualText == null && comparisionFailureExpectedText == null) {
                testProxy.setTestFailed(localizedMessage, stackTrace, isTestError);
            }
            else {
                testProxy.setTestFailed(localizedMessage, stackTrace, isTestError);
                logProblem("Comparison failure actual and expected texts should be both null or not null.\n"
                    + "Expected:\n"
                    + comparisionFailureExpectedText + "\n"
                    + "Actual:\n"
                    + comparisionFailureActualText);
            }

            // fire event
            fireOnTestFailed(testProxy);
        });
    }

    @Override
    public void onTestIgnored(@Nonnull TestIgnoredEvent testIgnoredEvent) {
        addToInvokeLater(() -> {
            String testName = testIgnoredEvent.getName();
            if (testName == null) {
                logProblem("TestIgnored event: no name");
            }
            String ignoreComment = testIgnoredEvent.getIgnoreComment();
            String stackTrace = testIgnoredEvent.getStacktrace();
            String fullTestName = getFullTestName(testName);
            SMTestProxy testProxy = getProxyByFullTestName(fullTestName);
            if (testProxy == null) {
                boolean debugMode = SMTestRunnerConnectionUtil.isInDebugMode();
                logProblem(
                    "Test wasn't started! " +
                        "TestIgnored event: name = {" + testName + "}, " +
                        "message = {" + ignoreComment + "}. " +
                        cannotFindFullTestNameMsg(fullTestName)
                );
                if (debugMode) {
                    return;
                }
                else {
                    // try to fix
                    // 1. report test opened
                    onTestStarted(new TestStartedEvent(testName, null));

                    // 2. report failure
                    testProxy = getProxyByFullTestName(fullTestName);
                }

            }
            if (testProxy == null) {
                return;
            }
            testProxy.setTestIgnored(ignoreComment, stackTrace);

            // fire event
            fireOnTestIgnored(testProxy);
        });
    }

    @Override
    public void onTestOutput(@Nonnull TestOutputEvent testOutputEvent) {
        addToInvokeLater(() -> {
            String testName = testOutputEvent.getName();
            String text = testOutputEvent.getText();
            boolean stdOut = testOutputEvent.isStdOut();
            String fullTestName = getFullTestName(testName);
            SMTestProxy testProxy = getProxyByFullTestName(fullTestName);
            if (testProxy == null) {
                logProblem("Test wasn't started! TestOutput event: name = {" + testName + "}, " +
                    "isStdOut = " + stdOut + ", " +
                    "text = {" + text + "}. " +
                    cannotFindFullTestNameMsg(fullTestName));
                return;
            }

            if (stdOut) {
                testProxy.addStdOutput(text, ProcessOutputTypes.STDOUT);
            }
            else {
                testProxy.addStdErr(text);
            }
        });
    }

    @Override
    public void onTestsCountInSuite( int count) {
        addToInvokeLater(() -> fireOnTestsCountInSuite(count));
    }

    @Nonnull
    protected final SMTestProxy getCurrentSuite() {
        SMTestProxy currentSuite = mySuitesStack.getCurrentSuite();

        if (currentSuite != null) {
            return currentSuite;
        }

        // current suite shouldn't be null otherwise test runner isn't correct
        // or may be we are in debug mode
        logProblem("Current suite is undefined. Root suite will be used.");
        myGetChildren = true;
        return myTestsRootProxy;

    }

    protected String getFullTestName(final String testName) {
        // Test name should be unique
        return testName;
    }

    protected int getRunningTestsQuantity() {
        return myRunningTestsFullNameToProxy.size();
    }

    @Nullable
    protected SMTestProxy getProxyByFullTestName(final String fullTestName) {
        return myRunningTestsFullNameToProxy.get(fullTestName);
    }

    @TestOnly
    protected void clearInternalSuitesStack() {
        mySuitesStack.clear();
    }

    private String cannotFindFullTestNameMsg(String fullTestName) {
        return "Cant find running test for ["
            + fullTestName
            + "]. Current running tests: {"
            + dumpRunningTestsNames() + "}";
    }

    private StringBuilder dumpRunningTestsNames() {
        Set<String> names = myRunningTestsFullNameToProxy.keySet();
        StringBuilder namesDump = new StringBuilder();
        for (String name : names) {
            namesDump.append('[').append(name).append(']').append(',');
        }
        return namesDump;
    }


    /*
     * Remove listeners,  etc
     */
    @Override
    public void dispose() {
        super.dispose();
        addToInvokeLater(() -> {
            disconnectListeners();
            if (!myRunningTestsFullNameToProxy.isEmpty()) {
                Application application = Application.get();
                if (!application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
                    logProblem("Not all events were processed! " + dumpRunningTestsNames());
                }
            }
            myRunningTestsFullNameToProxy.clear();
            mySuitesStack.clear();
        });
    }


    private SMTestProxy findCurrentTestOrSuite() {
        //if we can locate test - we will send output to it, otherwise to current test suite
        SMTestProxy currentProxy;
        if (myRunningTestsFullNameToProxy.size() == 1) {
            //current test
            currentProxy = myRunningTestsFullNameToProxy.values().iterator().next();
        }
        else {
            //current suite
            //
            // ProcessHandler can fire output available event before processStarted event
            currentProxy = mySuitesStack.isEmpty() ? myTestsRootProxy : getCurrentSuite();
        }
        return currentProxy;
    }
}
