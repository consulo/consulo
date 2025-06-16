// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.test.sm.runner;

import consulo.component.util.BuildNumber;
import consulo.disposer.Disposer;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.event.*;
import consulo.execution.util.ConsoleBuffer;
import consulo.logging.Logger;
import consulo.process.ProcessOutputTypes;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jetbrains.buildServer.messages.serviceMessages.*;

import java.text.ParseException;
import java.util.Map;

/**
 * This implementation also supports messages split in parts by early flush.
 * Implementation assumes that buffer is being flushed on line end or by timer,
 * i.e. incoming text contains no more than one line's end marker ('\r', '\n', or "\r\n")
 * (e.g. process was run with IDEA program's runner)
 *
 * @author Roman Chernyatchik
 */
public class OutputToGeneralTestEventsConverter implements ProcessOutputConsumer {
    private static final Logger LOG = Logger.getInstance(OutputToGeneralTestEventsConverter.class);
    private static final boolean USE_CYCLE_BUFFER = ConsoleBuffer.useCycleBuffer();

    private final MyServiceMessageVisitor myServiceMessageVisitor;
    private final String myTestFrameworkName;
    private final OutputLineSplitter mySplitter;

    private volatile GeneralTestEventsProcessor myProcessor;
    private boolean myPendingLineBreakFlag;
    private Runnable myTestingStartedHandler;
    private boolean myFirstTestingStartedEvent = true;

    public OutputToGeneralTestEventsConverter(@Nonnull String testFrameworkName, @Nonnull TestConsoleProperties consoleProperties) {
        this(testFrameworkName, consoleProperties.isEditable());
    }

    public OutputToGeneralTestEventsConverter(@Nonnull String testFrameworkName, boolean stdinEnabled) {
        myTestFrameworkName = testFrameworkName;
        myServiceMessageVisitor = new MyServiceMessageVisitor();
        mySplitter = new OutputLineSplitter(stdinEnabled) {
            @Override
            protected void onLineAvailable(@Nonnull String text, @Nonnull Key outputType, boolean tcLikeFakeOutput) {
                processConsistentText(text, outputType, tcLikeFakeOutput);
            }
        };
    }

    @Override
    public void setProcessor(@Nullable GeneralTestEventsProcessor processor) {
        myProcessor = processor;
    }

    protected GeneralTestEventsProcessor getProcessor() {
        return myProcessor;
    }

    @Override
    public void dispose() {
        setProcessor(null);
    }

    @Override
    public void process(String text, Key outputType) {
        mySplitter.process(text, outputType);
    }

    /**
     * Flashes the rest of stdout text buffer after output has been stopped
     */
    @Override
    public void flushBufferOnProcessTermination(int exitCode) {
        mySplitter.flush();
        if (myPendingLineBreakFlag) {
            fireOnUncapturedLineBreak();
        }
    }

    private void fireOnUncapturedLineBreak() {
        fireOnUncapturedOutput("\n", ProcessOutputTypes.STDOUT);
    }

    protected void processConsistentText(String text, Key outputType, boolean tcLikeFakeOutput) {
        int cycleBufferSize = ConsoleBuffer.getCycleBufferSize();
        if (USE_CYCLE_BUFFER && text.length() > cycleBufferSize) {
            StringBuilder builder = new StringBuilder(cycleBufferSize);
            builder.append(text, 0, cycleBufferSize - 105);
            builder.append("<...>");
            builder.append(text, text.length() - 100, text.length());
            text = builder.toString();
        }

        try {
            if (!processServiceMessages(text, outputType, myServiceMessageVisitor)) {
                if (myPendingLineBreakFlag) {
                    // output type for line break isn't important
                    // we may use any, e.g. current one
                    fireOnUncapturedLineBreak();
                    myPendingLineBreakFlag = false;
                }
                // Filters \n
                String outputToProcess = text;
                if (tcLikeFakeOutput && text.endsWith("\n")) {
                    // ServiceMessages protocol requires that every message
                    // should start with new line, so such behaviour may led to generating
                    // some number of useless \n.
                    //
                    // IDEA process handler flush output by size or line break
                    // So:
                    //  1. "a\n\nb\n" -> ["a\n", "\n", "b\n"]
                    //  2. "a\n##teamcity[..]\n" -> ["a\n", "#teamcity[..]\n"]
                    // We need distinguish 1) and 2) cases, in 2) first linebreak is redundant and must be ignored
                    // in 2) linebreak must be considered as output
                    // output will be in TestOutput message
                    // Lets set myPendingLineBreakFlag if we meet "\n" and then ignore it or apply depending on
                    // next output chunk
                    myPendingLineBreakFlag = true;
                    outputToProcess = outputToProcess.substring(0, outputToProcess.length() - 1);
                }
                //fire current output
                fireOnUncapturedOutput(outputToProcess, outputType);
            }
            else {
                myPendingLineBreakFlag = false;
            }
        }
        catch (ParseException e) {

            LOG.error(GeneralTestEventsProcessor.getTFrameworkPrefix(myTestFrameworkName) + "Error parsing text: [" + text + "]", e);
        }
    }

    protected boolean processServiceMessages(String text, Key outputType, ServiceMessageVisitor visitor) throws ParseException {
        // service message parser expects line like "##teamcity[ .... ]" without whitespaces in the end.
        ServiceMessage message = ServiceMessage.parse(text.trim());
        if (message != null) {
            message.visit(visitor);
        }
        return message != null;
    }


    private void fireOnTestStarted(@Nonnull TestStartedEvent testStartedEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestStarted(testStartedEvent);
        }
    }

    private void fireOnTestFailure(@Nonnull TestFailedEvent testFailedEvent) {
        assertNotNull(testFailedEvent.getLocalizedFailureMessage());

        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestFailure(testFailedEvent);
        }
    }

    private void fireOnTestIgnored(@Nonnull TestIgnoredEvent testIgnoredEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestIgnored(testIgnoredEvent);
        }
    }

    private void fireOnTestFinished(@Nonnull TestFinishedEvent testFinishedEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestFinished(testFinishedEvent);
        }
    }

    private void fireOnCustomProgressTestsCategory(String categoryName, int testsCount) {
        assertNotNull(categoryName);

        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            boolean disableCustomMode = StringUtil.isEmpty(categoryName);
            processor.onCustomProgressTestsCategory(
                disableCustomMode ? null : categoryName,
                disableCustomMode ? 0 : testsCount
            );
        }
    }

    private void fireOnCustomProgressTestStarted() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onCustomProgressTestStarted();
        }
    }

    private void fireOnCustomProgressTestFinished() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onCustomProgressTestFinished();
        }
    }

    private void fireOnCustomProgressTestFailed() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onCustomProgressTestFailed();
        }
    }

    private void fireOnTestFrameworkAttached() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestsReporterAttached();
        }
    }

    private void fireOnSuiteTreeNodeAdded(String testName, String locationHint, String id, String parentNodeId) {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onSuiteTreeNodeAdded(testName, locationHint, id, parentNodeId);
        }
    }


    private void fireRootPresentationAdded(String rootName, @Nullable String comment, String rootLocation) {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onRootPresentationAdded(rootName, comment, rootLocation);
        }
    }

    private void fireOnSuiteTreeStarted(String suiteName, String locationHint, String id, String parentNodeId) {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onSuiteTreeStarted(suiteName, locationHint, id, parentNodeId);
        }
    }

    private void fireOnSuiteTreeEnded(String suiteName) {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onSuiteTreeEnded(suiteName);
        }
    }

    private void fireOnBuildTreeEnded() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onBuildTreeEnded();
        }
    }

    private void fireOnTestOutput(@Nonnull TestOutputEvent testOutputEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestOutput(testOutputEvent);
        }
    }

    private void fireOnUncapturedOutput(String text, Key outputType) {
        assertNotNull(text);

        if (StringUtil.isEmpty(text)) {
            return;
        }

        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onUncapturedOutput(text, outputType);
        }
    }

    private void fireOnTestsCountInSuite(int count) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onTestsCountInSuite(count);
        }
    }

    private void fireOnSuiteStarted(@Nonnull TestSuiteStartedEvent suiteStartedEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onSuiteStarted(suiteStartedEvent);
        }
    }

    private void fireOnSuiteFinished(@Nonnull TestSuiteFinishedEvent suiteFinishedEvent) {
        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onSuiteFinished(suiteFinishedEvent);
        }
    }

    protected void fireOnErrorMsg(
        String localizedMessage,
        @Nullable String stackTrace,
        boolean isCritical
    ) {
        assertNotNull(localizedMessage);

        // local variable is used to prevent concurrent modification
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onError(localizedMessage, stackTrace, isCritical);
        }
    }

    private void assertNotNull(String s) {
        if (s == null) {
            LOG.error(GeneralTestEventsProcessor.getTFrameworkPrefix(myTestFrameworkName) + " @NotNull value is expected.");
        }
    }

    public void setTestingStartedHandler(@Nonnull Runnable testingStartedHandler) {
        myTestingStartedHandler = testingStartedHandler;
    }

    public void onStartTesting() {
    }

    public synchronized void startTesting() {
        myTestingStartedHandler.run();
        onStartTesting();
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onStartTesting();
        }
    }

    public synchronized void finishTesting() {
        GeneralTestEventsProcessor processor = myProcessor;
        if (processor != null) {
            processor.onFinishTesting();
            Disposer.dispose(processor);
        }
    }

    private class MyServiceMessageVisitor extends DefaultServiceMessageVisitor {
        private static final String TESTING_STARTED = "testingStarted";
        private static final String TESTING_FINISHED = "testingFinished";
        private static final String KEY_TESTS_COUNT = "testCount";
        private static final String ATTR_KEY_TEST_ERROR = "error";
        private static final String ATTR_KEY_TEST_COUNT = "count";
        private static final String ATTR_KEY_TEST_DURATION = "duration";
        private static final String ATTR_KEY_TEST_OUTPUT_FILE = "outputFile";
        private static final String ATTR_KEY_LOCATION_URL = "locationHint";
        private static final String ATTR_KEY_LOCATION_URL_OLD = "location";
        private static final String ATTR_KEY_STACKTRACE_DETAILS = "details";
        private static final String ATTR_KEY_DIAGNOSTIC = "diagnosticInfo";
        private static final String ATTR_KEY_CONFIG = "config";

        private static final String MESSAGE = "message";
        private static final String TEST_REPORTER_ATTACHED = "enteredTheMatrix";
        private static final String SUITE_TREE_STARTED = "suiteTreeStarted";
        private static final String SUITE_TREE_ENDED = "suiteTreeEnded";
        private static final String SUITE_TREE_NODE = "suiteTreeNode";
        private static final String BUILD_TREE_ENDED_NODE = "treeEnded";
        private static final String ROOT_PRESENTATION = "rootName";

        private static final String ATTR_KEY_STATUS = "status";
        private static final String ATTR_VALUE_STATUS_ERROR = "ERROR";
        private static final String ATTR_VALUE_STATUS_WARNING = "WARNING";
        private static final String ATTR_KEY_TEXT = "text";
        private static final String ATTR_KEY_ERROR_DETAILS = "errorDetails";
        private static final String ATTR_KEY_EXPECTED_FILE_PATH = "expectedFile";
        private static final String ATTR_KEY_ACTUAL_FILE_PATH = "actualFile";

        public static final String CUSTOM_STATUS = "customProgressStatus";
        private static final String ATTR_KEY_TEST_TYPE = "type";
        private static final String ATTR_KEY_TESTS_CATEGORY = "testsCategory";
        private static final String ATTR_VAL_TEST_STARTED = "testStarted";
        private static final String ATTR_VAL_TEST_FINISHED = "testFinished";
        private static final String ATTR_VAL_TEST_FAILED = "testFailed";

        @Override
        public void visitTestSuiteStarted(@Nonnull TestSuiteStarted suiteStarted) {
            String locationUrl = fetchTestLocation(suiteStarted);
            TestSuiteStartedEvent suiteStartedEvent = new TestSuiteStartedEvent(suiteStarted, locationUrl);
            fireOnSuiteStarted(suiteStartedEvent);
        }

        @Nullable
        private String fetchTestLocation(TestSuiteStarted suiteStarted) {
            Map<String, String> attrs = suiteStarted.getAttributes();
            String location = attrs.get(ATTR_KEY_LOCATION_URL);
            if (location == null) {
                // try old API
                String oldLocation = attrs.get(ATTR_KEY_LOCATION_URL_OLD);
                if (oldLocation != null) {
                    LOG.error(
                        GeneralTestEventsProcessor.getTFrameworkPrefix(myTestFrameworkName) +
                            "Test Runner API was changed for TeamCity 5.0 compatibility. " +
                            "Please use 'locationHint' attribute instead of 'location'."
                    );
                    return oldLocation;
                }
                return null;
            }
            return location;
        }

        @Override
        public void visitTestSuiteFinished(@Nonnull TestSuiteFinished suiteFinished) {
            TestSuiteFinishedEvent finishedEvent = new TestSuiteFinishedEvent(suiteFinished);
            fireOnSuiteFinished(finishedEvent);
        }

        @Override
        public void visitTestStarted(@Nonnull TestStarted testStarted) {
            // TODO
            // final String locationUrl = testStarted.getLocationHint();

            Map<String, String> attributes = testStarted.getAttributes();
            String locationUrl = attributes.get(ATTR_KEY_LOCATION_URL);
            TestStartedEvent testStartedEvent = new TestStartedEvent(testStarted, locationUrl);
            testStartedEvent.setConfig(attributes.get(ATTR_KEY_CONFIG) != null);
            fireOnTestStarted(testStartedEvent);
        }

        @Override
        public void visitTestFinished(@Nonnull TestFinished testFinished) {
            //TODO
            //final Integer duration = testFinished.getTestDuration();
            //fireOnTestFinished(testFinished.getTestName(), duration != null ? duration.intValue() : 0);

            String durationStr = testFinished.getAttributes().get(ATTR_KEY_TEST_DURATION);

            // Test duration in milliseconds or null if not reported
            Long duration = null;

            if (!StringUtil.isEmptyOrSpaces(durationStr)) {
                duration = convertToLong(durationStr, testFinished);
            }

            TestFinishedEvent testFinishedEvent = new TestFinishedEvent(testFinished, duration,
                testFinished.getAttributes().get(ATTR_KEY_TEST_OUTPUT_FILE)
            );
            fireOnTestFinished(testFinishedEvent);
        }

        @Override
        public void visitTestIgnored(@Nonnull TestIgnored testIgnored) {
            String stacktrace = testIgnored.getAttributes().get(ATTR_KEY_STACKTRACE_DETAILS);
            fireOnTestIgnored(new TestIgnoredEvent(testIgnored, stacktrace));
        }

        @Override
        public void visitTestStdOut(@Nonnull TestStdOut testStdOut) {
            fireOnTestOutput(new TestOutputEvent(testStdOut, testStdOut.getStdOut(), true));
        }

        @Override
        public void visitTestStdErr(@Nonnull TestStdErr testStdErr) {
            fireOnTestOutput(new TestOutputEvent(testStdErr, testStdErr.getStdErr(), false));
        }

        @Override
        public void visitTestFailed(@Nonnull TestFailed testFailed) {
            Map<String, String> attributes = testFailed.getAttributes();
            LOG.assertTrue(testFailed.getFailureMessage() != null, "No failure message for: " + myTestFrameworkName);
            boolean testError = attributes.get(ATTR_KEY_TEST_ERROR) != null;
            TestFailedEvent testFailedEvent = new TestFailedEvent(testFailed, testError,
                attributes.get(ATTR_KEY_EXPECTED_FILE_PATH),
                attributes.get(ATTR_KEY_ACTUAL_FILE_PATH)
            );
            fireOnTestFailure(testFailedEvent);
        }

        @Override
        public void visitPublishArtifacts(@Nonnull PublishArtifacts publishArtifacts) {
            //Do nothing
        }

        @Override
        public void visitProgressMessage(@Nonnull ProgressMessage progressMessage) {
            //Do nothing
        }

        @Override
        public void visitProgressStart(@Nonnull ProgressStart progressStart) {
            //Do nothing
        }

        @Override
        public void visitProgressFinish(@Nonnull ProgressFinish progressFinish) {
            //Do nothing
        }

        @Override
        public void visitBuildStatus(@Nonnull BuildStatus buildStatus) {
            //Do nothing
        }

        public void visitBuildNumber(@Nonnull BuildNumber buildNumber) {
            //Do nothing
        }

        @Override
        public void visitBuildStatisticValue(@Nonnull BuildStatisticValue buildStatsValue) {
            //Do nothing
        }

        @Override
        public void visitMessageWithStatus(@Nonnull Message msg) {
            Map<String, String> msgAttrs = msg.getAttributes();

            String text = msgAttrs.get(ATTR_KEY_TEXT);
            if (!StringUtil.isEmpty(text)) {
                // msg status
                String status = msgAttrs.get(ATTR_KEY_STATUS);
                if (status.equals(ATTR_VALUE_STATUS_ERROR)) {
                    // error msg

                    String stackTrace = msgAttrs.get(ATTR_KEY_ERROR_DETAILS);
                    fireOnErrorMsg(text, stackTrace, true);
                }
                else if (status.equals(ATTR_VALUE_STATUS_WARNING)) {
                    // warning msg

                    // let's show warning via stderr
                    String stackTrace = msgAttrs.get(ATTR_KEY_ERROR_DETAILS);
                    fireOnErrorMsg(text, stackTrace, false);
                }
                else {
                    // some other text

                    // we cannot pass output type here but it is a service message
                    // let's think that is was stdout
                    fireOnUncapturedOutput(text, ProcessOutputTypes.STDOUT);
                }
            }
        }

        @Override
        public void visitServiceMessage(@Nonnull ServiceMessage msg) {
            String name = msg.getMessageName();

            if (LOG.isDebugEnabled()) {
                LOG.debug(msg.asString());
            }

            if (TESTING_STARTED.equals(name)) {
                // Since a test reporter may not emit "testingStarted"/"testingFinished" events,
                // startTesting() is already invoked before starting processing messages.
                if (!myFirstTestingStartedEvent) {
                    startTesting();
                }
                myFirstTestingStartedEvent = false;
            }
            else if (TESTING_FINISHED.equals(name)) {
                finishTesting();
            }
            else if (KEY_TESTS_COUNT.equals(name)) {
                processTestCountInSuite(msg);
            }
            else if (CUSTOM_STATUS.equals(name)) {
                processCustomStatus(msg);
            }
            else if (MESSAGE.equals(name)) {
                Map<String, String> msgAttrs = msg.getAttributes();

                String text = msgAttrs.get(ATTR_KEY_TEXT);
                if (!StringUtil.isEmpty(text)) {
                    // some other text

                    // we cannot pass output type here but it is a service message
                    // let's think that is was stdout
                    fireOnUncapturedOutput(text, ProcessOutputTypes.STDOUT);
                }
            }
            else if (TEST_REPORTER_ATTACHED.equals(name)) {
                fireOnTestFrameworkAttached();
            }
            else if (SUITE_TREE_STARTED.equals(name)) {
                fireOnSuiteTreeStarted(
                    msg.getAttributes().get("name"),
                    msg.getAttributes().get(ATTR_KEY_LOCATION_URL),
                    TreeNodeEvent.getNodeId(msg),
                    msg.getAttributes().get("parentNodeId")
                );
            }
            else if (SUITE_TREE_ENDED.equals(name)) {
                fireOnSuiteTreeEnded(msg.getAttributes().get("name"));
            }
            else if (SUITE_TREE_NODE.equals(name)) {
                fireOnSuiteTreeNodeAdded(
                    msg.getAttributes().get("name"),
                    msg.getAttributes().get(ATTR_KEY_LOCATION_URL),
                    TreeNodeEvent.getNodeId(msg),
                    msg.getAttributes().get("parentNodeId")
                );
            }
            else if (BUILD_TREE_ENDED_NODE.equals(name)) {
                fireOnBuildTreeEnded();
            }
            else if (ROOT_PRESENTATION.equals(name)) {
                Map<String, String> attributes = msg.getAttributes();
                fireRootPresentationAdded(attributes.get("name"), attributes.get("comment"), attributes.get("location"));
            }
            else {
                GeneralTestEventsProcessor.logProblem(LOG, "Unexpected service message:" + name, myTestFrameworkName);
            }
        }

        private void processTestCountInSuite(ServiceMessage msg) {
            String countStr = msg.getAttributes().get(ATTR_KEY_TEST_COUNT);
            fireOnTestsCountInSuite(convertToInt(countStr, msg));
        }

        private int convertToInt(String countStr, ServiceMessage msg) {
            int count = 0;
            try {
                count = Integer.parseInt(countStr);
            }
            catch (NumberFormatException ex) {
                String diagnosticInfo = msg.getAttributes().get(ATTR_KEY_DIAGNOSTIC);
                LOG.error(
                    GeneralTestEventsProcessor.getTFrameworkPrefix(myTestFrameworkName) +
                        "Parse integer error." + (diagnosticInfo == null ? "" : " " + diagnosticInfo),
                    ex
                );
            }
            return count;
        }

        private long convertToLong(String countStr, @Nonnull ServiceMessage msg) {
            long count = 0;
            try {
                count = Long.parseLong(countStr);
            }
            catch (NumberFormatException ex) {
                String diagnosticInfo = msg.getAttributes().get(ATTR_KEY_DIAGNOSTIC);
                LOG.error(
                    GeneralTestEventsProcessor.getTFrameworkPrefix(myTestFrameworkName) +
                        "Parse long error." + (diagnosticInfo == null ? "" : " " + diagnosticInfo),
                    ex
                );
            }
            return count;
        }

        private void processCustomStatus(ServiceMessage msg) {
            Map<String, String> attrs = msg.getAttributes();
            String msgType = attrs.get(ATTR_KEY_TEST_TYPE);
            if (msgType != null) {
                switch (msgType) {
                    case ATTR_VAL_TEST_STARTED -> fireOnCustomProgressTestStarted();
                    case ATTR_VAL_TEST_FINISHED -> fireOnCustomProgressTestFinished();
                    case ATTR_VAL_TEST_FAILED -> fireOnCustomProgressTestFailed();
                }
                return;
            }
            String testsCategory = attrs.get(ATTR_KEY_TESTS_CATEGORY);
            if (testsCategory != null) {
                String countStr = msg.getAttributes().get(ATTR_KEY_TEST_COUNT);
                fireOnCustomProgressTestsCategory(testsCategory, convertToInt(countStr, msg));

                //noinspection UnnecessaryReturnStatement
                return;
            }
        }
    }
}
