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

import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.test.*;
import consulo.execution.test.sm.SMStacktraceParser;
import consulo.execution.test.sm.SMStacktraceParserEx;
import consulo.execution.test.sm.runner.event.TestFailedEvent;
import consulo.execution.test.sm.runner.state.*;
import consulo.execution.test.sm.ui.TestsPresentationUtil;
import consulo.execution.test.stacktrace.DiffHyperlink;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.EditSourceUtil;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a test result tree node.
 * Not thread-safe. All methods should be called in EDT only.
 *
 * @author Roman Chernyatchik
 */
public class SMTestProxy extends AbstractTestProxy {
    public static final Key<String> NODE_ID = Key.create("test.proxy.id");

    private static final Logger LOG = Logger.getInstance(SMTestProxy.class);

    private final String myName;
    private boolean myIsSuite;
    private final String myLocationUrl;
    private final String myMetainfo;
    private final boolean myPreservePresentableName;

    private List<SMTestProxy> myChildren;
    private SMTestProxy myParent;

    private AbstractState myState = NotRunState.getInstance();
    private Long myDuration = null; // duration is unknown
    private boolean myDurationIsCached = false; // is used for separating unknown and unset duration
    private boolean myHasCriticalErrors = false;
    private boolean myHasPassedTests = false;
    private boolean myHasPassedTestsCached = false;

    private String myStacktrace;

    private boolean myIsEmptyIsCached = false; // is used for separating unknown and unset values
    private boolean myIsEmpty = true;
    private SMTestLocator myLocator = null;
    private Printer myPreferredPrinter = null;
    private String myPresentableName;
    private boolean myConfig = false;
    //false:: printables appear as soon as they are discovered in the output; true :: predefined test structure
    private boolean myTreeBuildBeforeStart = false;

    public SMTestProxy(String testName, boolean isSuite, @Nullable String locationUrl) {
        this(testName, isSuite, locationUrl, false);
    }

    public SMTestProxy(String testName, boolean isSuite, @Nullable String locationUrl, boolean preservePresentableName) {
        this(testName, isSuite, locationUrl, null, preservePresentableName);
    }

    public SMTestProxy(
        String testName,
        boolean isSuite,
        @Nullable String locationUrl,
        @Nullable String metainfo,
        boolean preservePresentableName
    ) {
        myName = testName;
        myIsSuite = isSuite;
        myLocationUrl = locationUrl;
        myMetainfo = metainfo;
        myPreservePresentableName = preservePresentableName;
    }

    public boolean isPreservePresentableName() {
        return myPreservePresentableName;
    }

    public void setLocator(@Nonnull SMTestLocator testLocator) {
        myLocator = testLocator;
    }

    public void setConfig(boolean config) {
        myConfig = config;
    }

    public void setPreferredPrinter(@Nonnull Printer preferredPrinter) {
        myPreferredPrinter = preferredPrinter;
    }

    @Override
    public boolean isInProgress() {
        return myState.isInProgress();
    }

    @Override
    public boolean isDefect() {
        return myState.isDefect();
    }

    @Override
    public boolean shouldRun() {
        return true;
    }

    @Override
    public int getMagnitude() {
        // Is used by some of Tests Filters
        //WARN: It is Hack, see PoolOfTestStates, API is necessary
        return getMagnitudeInfo().getValue();
    }

    public TestStateInfo.Magnitude getMagnitudeInfo() {
        return myState.getMagnitude();
    }

    public boolean hasErrors() {
        return myHasCriticalErrors;
    }

    /**
     * @return true if the state is final (PASSED, FAILED, IGNORED, TERMINATED)
     */
    public boolean isFinal() {
        return myState.isFinal();
    }

    private void setStacktraceIfNotSet(@Nullable String stacktrace) {
        if (myStacktrace == null) {
            myStacktrace = stacktrace;
        }
    }

    @Override
    public boolean isLeaf() {
        return myChildren == null || myChildren.isEmpty();
    }

    @Override
    public boolean hasPassedTests() {
        if (myHasPassedTestsCached) {
            return myHasPassedTests;
        }
        boolean hasPassedTests = calcPassedTests();
        boolean canCache = !myState.isInProgress() && myState.wasLaunched();
        if (canCache) {
            myHasPassedTests = hasPassedTests;
            myHasPassedTestsCached = true;
        }
        return hasPassedTests;

    }

    @Override
    public boolean isInterrupted() {
        return myState.wasTerminated();
    }

    private boolean calcPassedTests() {
        if (isPassed()) {
            return true;
        }
        for (SMTestProxy child : getChildren()) {
            if (child.hasPassedTests()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isIgnored() {
        return myState.getMagnitude() == TestStateInfo.Magnitude.IGNORED_INDEX;
    }

    @Override
    public boolean isPassed() {
        return myState.getMagnitude() == TestStateInfo.Magnitude.SKIPPED_INDEX ||
            myState.getMagnitude() == TestStateInfo.Magnitude.COMPLETE_INDEX ||
            myState.getMagnitude() == TestStateInfo.Magnitude.PASSED_INDEX;
    }

    @RequiredUIAccess
    public void addChild(@Nonnull SMTestProxy child) {
        UIAccess.assertIsUIThread();

        if (myChildren == null) {
            myChildren = new ArrayList<>(4);
        }
        myChildren.add(child);

        // add printable
        //
        // add link to child's future output in correct place
        // actually if after this suite will obtain output
        // it will place it after this child and before future child
        addLast(child);

        // add child
        //
        //TODO reset children cache
        child.setParent(this);

        boolean printOwnContentOnly = this instanceof SMRootTestProxy rootTestProxy && rootTestProxy.shouldPrintOwnContentOnly();
        if (!printOwnContentOnly) {
            child.setPrinter(myPrinter);
        }
        if (myPreferredPrinter != null && child.myPreferredPrinter == null) {
            child.setPreferredPrinter(myPreferredPrinter);
        }
    }

    @Nullable
    private Printer getRightPrinter(@Nullable Printer printer) {
        if (myPreferredPrinter != null && printer != null) {
            return myPreferredPrinter;
        }
        return printer;
    }

    @Override
    public void setPrinter(Printer printer) {
        super.setPrinter(getRightPrinter(printer));
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isConfig() {
        return myConfig;
    }

    @Override
    @Nullable
    public Location getLocation(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope) {
        //determines location of test proxy
        return getLocation(project, searchScope, myLocationUrl);
    }

    protected Location getLocation(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope, String locationUrl) {
        if (locationUrl != null && myLocator != null) {
            String protocolId = VirtualFileManager.extractProtocol(locationUrl);
            if (protocolId != null) {
                String path = VirtualFileManager.extractPath(locationUrl);
                if (!DumbService.isDumb(project) || DumbService.isDumbAware(myLocator)) {
                    return DumbService.getInstance(project).computeWithAlternativeResolveEnabled(() -> {
                        List<Location> locations = myLocator.getLocation(protocolId, path, myMetainfo, project, searchScope);
                        return !locations.isEmpty() ? locations.get(0) : null;
                    });
                }
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Navigatable getDescriptor(@Nullable Location location, @Nonnull TestConsoleProperties properties) {
        // by location gets navigatable element.
        // It can be file or place in file (e.g. when OPEN_FAILURE_LINE is enabled)
        if (location == null) {
            return null;
        }

        String stacktrace = myStacktrace;
        if (stacktrace != null && properties instanceof SMStacktraceParser stacktraceParser && isLeaf()) {
            Navigatable result = stacktraceParser instanceof SMStacktraceParserEx stacktraceParserEx
                ? stacktraceParserEx.getErrorNavigatable(location, stacktrace)
                : stacktraceParser.getErrorNavigatable(location.getProject(), stacktrace);
            if (result != null) {
                return result;
            }
        }

        return EditSourceUtil.getDescriptor(location.getPsiElement());
    }

    public boolean isSuite() {
        return myIsSuite;
    }

    @Override
    public SMTestProxy getParent() {
        return myParent;
    }

    @Override
    public List<? extends SMTestProxy> getChildren() {
        return myChildren != null ? myChildren : Collections.emptyList();
    }

    @Override
    public List<SMTestProxy> getAllTests() {
        List<SMTestProxy> allTests = new ArrayList<>();

        allTests.add(this);

        for (SMTestProxy child : getChildren()) {
            allTests.addAll(child.getAllTests());
        }

        return allTests;
    }

    public void setStarted() {
        myState = !myIsSuite ? TestInProgressState.TEST : new SuiteInProgressState(this);
    }

    public void setSuiteStarted() {
        myState = new SuiteInProgressState(this);
        if (!myIsSuite) {
            myIsSuite = true;
        }
    }

    /**
     * Calculates and caches duration of test or suite
     *
     * @return null if duration is unknown, otherwise duration value in milliseconds;
     */
    @Nullable
    @Override
    public Long getDuration() {
        // Returns duration value for tests
        // or cached duration for suites
        if (myDurationIsCached || !isSuite()) {
            return myDuration;
        }

        //For suites counts and caches durations of its children. Also it evaluates partial duration,
        //i.e. if duration is unknown it will be ignored in summary value.
        //If duration for all children is unknown summary duration will be also unknown
        //if one of children is ignored - it's duration will be 0 and if child wasn't run,
        //then it's duration will be unknown
        myDuration = calcSuiteDuration();
        myDurationIsCached = true;

        return myDuration;
    }

    @Nullable
    @Override
    public String getDurationString(TestConsoleProperties consoleProperties) {
        switch (getMagnitudeInfo()) {
            case PASSED_INDEX:
            case RUNNING_INDEX:
                return !isSubjectToHide(consoleProperties) ? getDurationString() : null;
            case COMPLETE_INDEX:
            case FAILED_INDEX:
            case ERROR_INDEX:
            case IGNORED_INDEX:
            case SKIPPED_INDEX:
            case TERMINATED_INDEX:
                return getDurationString();
            default:
                return null;
        }
    }

    private boolean isSubjectToHide(TestConsoleProperties consoleProperties) {
        return TestConsoleProperties.HIDE_PASSED_TESTS.value(consoleProperties) && getParent() != null && !isDefect();
    }

    private String getDurationString() {
        Long duration = getDuration();
        return duration != null ? StringUtil.formatDuration(duration, "\u2009") : null;
    }

    @Override
    public boolean shouldSkipRootNodeForExport() {
        return true;
    }

    /**
     * Sets duration of test
     *
     * @param duration In milliseconds
     */
    public void setDuration(long duration) {
        if (!isSuite()) {
            invalidateCachedDurationForContainerSuites(duration - (myDuration != null ? myDuration : 0));
            myDurationIsCached = true;
            myDuration = (duration >= 0) ? duration : null;
            return;
        }
        else {
            invalidateCachedDurationForContainerSuites(-1);
        }

        // Not allow to directly set duration for suites.
        // It should be the sum of children. This requirement is only
        // for safety of current model and may be changed
        LOG.warn("Unsupported operation");
    }

    public void setFinished() {
        if (myState.isFinal()) {
            // we shouldn't fire new printable because final state
            // has been already fired
            return;
        }

        if (!isSuite()) {
            // if isn't in other finished state (ignored, failed or passed)
            myState = TestPassedState.INSTANCE;
        }
        else {
            //Test Suite
            myState = determineSuiteStateOnFinished();
        }
        // prints final state additional info
        fireOnNewPrintable(myState);
    }

    public void setTestFailed(@Nonnull String localizedMessage, @Nullable String stackTrace, boolean testError) {
        setStacktraceIfNotSet(stackTrace);
        TestFailedState failedState = new TestFailedState(localizedMessage, stackTrace);
        if (myState instanceof TestComparisionFailedState testComparisionFailedState) {
            CompoundTestFailedState states = new CompoundTestFailedState(localizedMessage, stackTrace);
            states.addFailure(testComparisionFailedState);
            states.addFailure(failedState);
            fireOnNewPrintable(failedState);
            myState = states;
        }
        else if (myState instanceof CompoundTestFailedState compoundTestFailedState) {
            compoundTestFailedState.addFailure(failedState);
            fireOnNewPrintable(failedState);
        }
        else if (myState instanceof TestFailedState testFailedState) {
            testFailedState.addError(localizedMessage, stackTrace, myPrinter);
        }
        else {
            myState = testError ? new TestErrorState(localizedMessage, stackTrace) : failedState;
            fireOnNewPrintable(myState);
        }
    }

    public void setTestComparisonFailed(
        @Nonnull String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText
    ) {
        setTestComparisonFailed(localizedMessage, stackTrace, actualText, expectedText, null, null);
    }

    public void setTestComparisonFailed(
        @Nonnull String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText,
        @Nonnull TestFailedEvent event
    ) {
        TestComparisionFailedState comparisionFailedState =
            setTestComparisonFailed(
                localizedMessage,
                stackTrace,
                actualText,
                expectedText,
                event.getExpectedFilePath(),
                event.getActualFilePath()
            );
        comparisionFailedState.setToDeleteExpectedFile(event.isExpectedFileTemp());
        comparisionFailedState.setToDeleteActualFile(event.isActualFileTemp());
    }

    public TestComparisionFailedState setTestComparisonFailed(
        @Nonnull String localizedMessage,
        @Nullable String stackTrace,
        @Nonnull String actualText,
        @Nonnull String expectedText,
        @Nullable String expectedFilePath,
        @Nullable String actualFilePath
    ) {
        setStacktraceIfNotSet(stackTrace);
        TestComparisionFailedState comparisionFailedState =
            new TestComparisionFailedState(localizedMessage, stackTrace, actualText, expectedText, expectedFilePath, actualFilePath);
        if (myState instanceof CompoundTestFailedState compoundTestFailedState) {
            compoundTestFailedState.addFailure(comparisionFailedState);
        }
        else if (myState instanceof TestFailedState testFailedState) {
            CompoundTestFailedState states = new CompoundTestFailedState(localizedMessage, stackTrace);
            states.addFailure(testFailedState);
            states.addFailure(comparisionFailedState);
            myState = states;
        }
        else {
            myState = comparisionFailedState;
        }
        fireOnNewPrintable(comparisionFailedState);
        return comparisionFailedState;
    }

    @Override
    public void dispose() {
        if (myState instanceof TestFailedState testFailedState) {
            Disposer.dispose(testFailedState);
        }

        super.dispose();
    }

    public void setTestIgnored(@Nullable String ignoreComment, @Nullable String stackTrace) {
        setStacktraceIfNotSet(stackTrace);
        myState = new TestIgnoredState(ignoreComment, stackTrace);
        fireOnNewPrintable(myState);
    }

    public void setParent(@Nullable SMTestProxy parent) {
        myParent = parent;
    }

    public List<? extends SMTestProxy> collectChildren(@Nullable Filter<SMTestProxy> filter) {
        return filterChildren(filter, collectChildren());
    }

    public List<? extends SMTestProxy> collectChildren() {
        List<? extends SMTestProxy> allChildren = getChildren();

        List<SMTestProxy> result = new ArrayList<>();

        result.addAll(allChildren);

        for (SMTestProxy p : allChildren) {
            result.addAll(p.collectChildren());
        }

        return result;
    }

    public List<? extends SMTestProxy> getChildren(@Nullable Filter<? super SMTestProxy> filter) {
        return filterChildren(filter, getChildren());
    }

    protected void addAfterLastPassed(Printable printable) {
        if (myTreeBuildBeforeStart) {
            int idx = 0;
            synchronized (myNestedPrintables) {
                for (Printable proxy : myNestedPrintables) {
                    if (proxy instanceof SMTestProxy testProxy && !testProxy.isFinal()) {
                        break;
                    }
                    idx++;
                }
            }
            insert(printable, idx);
        }
        else {
            addLast(printable);
        }
    }

    public void setTreeBuildBeforeStart() {
        myTreeBuildBeforeStart = true;
    }

    private static List<? extends SMTestProxy> filterChildren(
        @Nullable Filter<? super SMTestProxy> filter,
        List<? extends SMTestProxy> allChildren
    ) {
        if (filter == Filter.NO_FILTER || filter == null) {
            return allChildren;
        }

        List<SMTestProxy> selectedChildren = new ArrayList<>();
        for (SMTestProxy child : allChildren) {
            if (filter.shouldAccept(child)) {
                selectedChildren.add(child);
            }
        }

        if ((selectedChildren.isEmpty())) {
            return Collections.emptyList();
        }

        return selectedChildren;
    }

    public boolean wasLaunched() {
        return myState.wasLaunched();
    }

    /**
     * Prints this proxy and all its children on given printer
     *
     * @param printer Printer
     */
    @Override
    public void printOn(Printer printer) {
        Printer rightPrinter = getRightPrinter(printer);
        super.printOn(rightPrinter);
        printState(myState, rightPrinter);
    }

    @Override
    public void printOwnPrintablesOn(Printer printer) {
        if (isLeaf()) {
            super.printOn(printer);
        }
        else {
            super.printOwnPrintablesOn(printer);
        }
        printState(myState, printer);
    }

    private static void printState(AbstractState oldState, Printer rightPrinter) {
        invokeInAlarm(() -> {
            //Tests State, that provide and formats additional output
            oldState.printOn(rightPrinter);
        });
    }

    public void addStdOutput(String output, Key outputType) {
        addOutput(output, outputType);
    }

    public void addStdErr(String output) {
        addOutput(output, ProcessOutputTypes.STDERR);
    }

    public void addOutput(@Nonnull String output, @Nonnull Key outputType) {
        addAfterLastPassed(printer -> printer.print(output, ConsoleViewContentType.getConsoleViewType(outputType)));
    }

    public void addError(String output, @Nullable String stackTrace, boolean isCritical) {
        myHasCriticalErrors = isCritical;
        if (isCritical) {
            invalidateCachedHasErrorMark();
        }
        setStacktraceIfNotSet(stackTrace);

        addAfterLastPassed(printer -> {
            String errorText = TestFailedState.buildErrorPresentationText(output, stackTrace);
            if (errorText != null) {
                TestFailedState.printError(printer, Collections.singletonList(errorText));
            }
        });
    }

    private void invalidateCachedHasErrorMark() {
        myHasCriticalErrors = true;
        // Invalidates hasError state of container suite
        SMTestProxy containerSuite = getParent();
        if (containerSuite != null && !containerSuite.hasErrors()) {
            containerSuite.invalidateCachedHasErrorMark();
        }
    }

    public void addSystemOutput(String output) {
        addOutput(output, ProcessOutputTypes.SYSTEM);
    }

    @Nonnull
    public String getPresentableName() {
        if (myPresentableName == null) {
            if (myPreservePresentableName) {
                myPresentableName = TestsPresentationUtil.getPresentableNameTrimmedOnly(this);
            }
            else {
                myPresentableName = TestsPresentationUtil.getPresentableName(this);
            }
        }
        return myPresentableName;
    }

    @Override
    @Nullable
    public DiffHyperlink getDiffViewerProvider() {
        if (myState instanceof TestComparisionFailedState testComparisionFailedState) {
            return testComparisionFailedState.getHyperlink();
        }

        if (myState instanceof CompoundTestFailedState compoundTestFailedState) {
            return compoundTestFailedState.getHyperlinks().get(0);
        }

        if (myChildren != null) {
            for (SMTestProxy child : myChildren) {
                if (!child.isDefect()) {
                    continue;
                }
                DiffHyperlink provider = child.getDiffViewerProvider();
                if (provider != null) {
                    return provider;
                }
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public List<DiffHyperlink> getDiffViewerProviders() {
        if (myState instanceof CompoundTestFailedState compoundTestFailedState) {
            return compoundTestFailedState.getHyperlinks();
        }
        return super.getDiffViewerProviders();
    }

    @Override
    public String toString() {
        return getPresentableName();
    }

    /**
     * Process was terminated
     */
    public void setTerminated() {
        if (myState.isFinal()) {
            return;
        }
        myState = TerminatedState.INSTANCE;
        List<? extends SMTestProxy> children = getChildren();
        for (SMTestProxy child : children) {
            child.setTerminated();
        }
        fireOnNewPrintable(myState);
    }

    public boolean wasTerminated() {
        return myState.wasTerminated();
    }

    @Override
    @Nullable
    public String getLocationUrl() {
        return myLocationUrl;
    }

    @Override
    @Nullable
    public String getMetainfo() {
        return myMetainfo;
    }

    /**
     * Check if suite contains error tests or suites
     *
     * @return True if contains
     */
    private boolean containsErrorTests() {
        List<? extends SMTestProxy> children = getChildren();
        for (SMTestProxy child : children) {
            if (child.getMagnitudeInfo() == TestStateInfo.Magnitude.ERROR_INDEX) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFailedTests() {
        List<? extends SMTestProxy> children = getChildren();
        for (SMTestProxy child : children) {
            if (child.getMagnitudeInfo() == TestStateInfo.Magnitude.FAILED_INDEX) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines site state after it has been finished
     *
     * @return New state
     */
    protected AbstractState determineSuiteStateOnFinished() {
        AbstractState state;
        if (isLeaf()) {
            state = SuiteFinishedState.EMPTY_LEAF_SUITE;
        }
        else if (isEmptySuite()) {
            state = SuiteFinishedState.EMPTY_SUITE;
        }
        else {
            if (isDefect()) {
                // Test suit contains errors if at least one of its tests contains error
                if (containsErrorTests()) {
                    state = SuiteFinishedState.ERROR_SUITE;
                }
                else {
                    // if suite contains failed tests - all suite should be
                    // consider as failed
                    state = containsFailedTests()
                        ? SuiteFinishedState.FAILED_SUITE
                        : SuiteFinishedState.WITH_IGNORED_TESTS_SUITE;
                }
            }
            else {
                state = SuiteFinishedState.PASSED_SUITE;
            }
        }
        return state;
    }

    public boolean isEmptySuite() {
        if (myIsEmptyIsCached) {
            return myIsEmpty;
        }

        if (!isSuite()) {
            // test - no matter what we will return
            myIsEmpty = true;
            myIsEmptyIsCached = true;
            return true;
        }

        myIsEmpty = true;
        List<? extends SMTestProxy> allTestCases = getChildren();
        for (SMTestProxy testOrSuite : allTestCases) {
            if (testOrSuite.isSuite()) {
                // suite
                if (!testOrSuite.isEmptySuite()) {
                    // => parent suite isn't empty
                    myIsEmpty = false;
                    myIsEmptyIsCached = true;
                    break;
                }
                // all suites are empty
                myIsEmpty = true;
                // we can cache only final state, otherwise test may be added
                myIsEmptyIsCached = myState.isFinal();
            }
            else {
                // test => parent suite isn't empty
                myIsEmpty = false;
                myIsEmptyIsCached = true;
                break;
            }
        }
        return myIsEmpty;
    }


    @Nullable
    private Long calcSuiteDuration() {
        long partialDuration = 0;
        boolean durationOfChildrenIsUnknown = true;

        for (SMTestProxy child : getChildren()) {
            Long duration = child.getDuration();
            if (duration != null) {
                durationOfChildrenIsUnknown = false;
                partialDuration += duration;
            }
        }
        // Lets convert partial duration in integer object. Negative partial duration
        // means that duration of all children is unknown
        return durationOfChildrenIsUnknown ? null : partialDuration;
    }

    /**
     * Recursively invalidates cached duration for container(parent) suites or updates their value
     *
     * @param duration
     */
    private void invalidateCachedDurationForContainerSuites(long duration) {
        if (duration >= 0) {
            if (myDuration == null) {
                myDuration = duration;
            }
            else {
                myDuration += duration;
            }
        }
        else {
            // Invalidates duration of this suite
            myDuration = null;
            myDurationIsCached = false;
        }

        // Invalidates duration of container suite
        SMTestProxy containerSuite = getParent();
        if (containerSuite != null) {
            containerSuite.invalidateCachedDurationForContainerSuites(duration);
        }
    }

    public SMRootTestProxy getRoot() {
        SMTestProxy parent = getParent();
        while (parent != null && !(parent instanceof SMRootTestProxy)) {
            parent = parent.getParent();
        }
        return parent != null ? (SMRootTestProxy) parent : null;
    }

    public static class SMRootTestProxy extends SMTestProxy implements TestProxyRoot {
        private boolean myTestsReporterAttached; // false by default

        private String myPresentation;
        private String myComment;
        private String myRootLocationUrl;
        private ProcessHandler myHandler;
        private boolean myShouldPrintOwnContentOnly = false;

        public SMRootTestProxy() {
            this(false);
        }

        public SMRootTestProxy(boolean preservePresentableName) {
            super("[root]", true, null, preservePresentableName);
        }

        public void setTestsReporterAttached() {
            myTestsReporterAttached = true;
        }

        public boolean isTestsReporterAttached() {
            return myTestsReporterAttached;
        }

        @Override
        public String getPresentation() {
            return myPresentation;
        }

        public void setPresentation(String presentation) {
            myPresentation = presentation;
        }

        public void setComment(String comment) {
            myComment = comment;
        }

        @Override
        public String getComment() {
            return myComment;
        }

        public void setRootLocationUrl(String locationUrl) {
            myRootLocationUrl = locationUrl;
        }

        @Override
        public String getRootLocation() {
            return myRootLocationUrl;
        }

        public ProcessHandler getHandler() {
            return myHandler;
        }

        @Override
        public void setHandler(ProcessHandler handler) {
            myHandler = handler;
        }

        @Nullable
        @Override
        public Location getLocation(@Nonnull Project project, @Nonnull GlobalSearchScope searchScope) {
            return myRootLocationUrl != null ? super.getLocation(project, searchScope, myRootLocationUrl)
                : super.getLocation(project, searchScope);
        }

        @Override
        protected AbstractState determineSuiteStateOnFinished() {
            if (isLeaf() && !isTestsReporterAttached()) {
                return SuiteFinishedState.TESTS_REPORTER_NOT_ATTACHED;
            }
            return super.determineSuiteStateOnFinished();
        }

        public void testingRestarted() {
            if (!getChildren().isEmpty()) {
                getChildren().clear();
            }
            clear();
        }

        boolean shouldPrintOwnContentOnly() {
            return myShouldPrintOwnContentOnly;
        }

        public void setShouldPrintOwnContentOnly(boolean shouldPrintOwnContentOnly) {
            myShouldPrintOwnContentOnly = shouldPrintOwnContentOnly;
        }

        @Override
        public void printOn(@Nonnull Printer printer) {
            if (myShouldPrintOwnContentOnly) {
                printOwnPrintablesOn(printer, false);
            }
            else {
                super.printOn(printer);
            }
        }
    }
}