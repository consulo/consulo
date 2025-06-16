/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.execution.process.AnsiEscapeDecoder;
import consulo.execution.test.*;
import consulo.execution.test.sm.localize.SMTestLocalize;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ProcessOutputTypes;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * @author Roman Chernyatchik
 */
public class TestsPresentationUtil {
    private static final String DOUBLE_SPACE = "  ";
    private static final String COLON = ": ";
    public static final SimpleTextAttributes PASSED_ATTRIBUTES =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, TestsUIUtil.PASSED_COLOR);
    public static final SimpleTextAttributes DEFFECT_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color.RED);
    public static final SimpleTextAttributes TERMINATED_ATTRIBUTES =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color.ORANGE);
    private static final String UNKNOWN_TESTS_COUNT = "<...>";
    static final String DEFAULT_TESTS_CATEGORY = "Tests";

    private TestsPresentationUtil() {
    }

    public static String getProgressStatus_Text(
        long startTime,
        long endTime,
        int testsTotal,
        int testsCount,
        int failuresCount,
        @Nullable Set<String> allCategories,
        boolean isFinished
    ) {
        StringBuilder sb = new StringBuilder();
        if (endTime == 0) {
            sb.append(SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsRunning());
        }
        else {
            sb.append(SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsDone());
        }

        if (allCategories != null) {
            // if all categories is just one default tests category - let's do not add prefixes

            if (hasNonDefaultCategories(allCategories)) {

                sb.append(' ');
                boolean first = true;
                for (String category : allCategories) {
                    if (StringUtil.isEmpty(category)) {
                        continue;
                    }

                    // separator
                    if (!first) {
                        sb.append(", ");

                    }

                    // first symbol - to lower case
                    char firstChar = category.charAt(0);
                    sb.append(first ? firstChar : Character.toLowerCase(firstChar));

                    sb.append(category.substring(1));
                    first = false;
                }
            }
        }

        sb.append(' ').append(testsCount).append(' ');
        sb.append(SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsOf());
        sb.append(' ').append(testsTotal != 0 ? testsTotal
            : !isFinished ? UNKNOWN_TESTS_COUNT : 0);

        if (failuresCount > 0) {
            sb.append(DOUBLE_SPACE);
            sb.append(SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsFailed());
            sb.append(' ').append(failuresCount);
        }
        if (endTime != 0) {
            long time = endTime - startTime;
            sb.append(DOUBLE_SPACE);
            sb.append('(').append(StringUtil.formatDuration(time)).append(')');
        }
        sb.append(DOUBLE_SPACE);

        return sb.toString();
    }

    public static boolean hasNonDefaultCategories(@Nullable Set<String> allCategories) {
        if (allCategories == null) {
            return false;
        }
        return allCategories.size() > 1 || (allCategories.size() == 1 && !DEFAULT_TESTS_CATEGORY.equals(allCategories.iterator().next()));
    }

    public static void formatRootNodeWithChildren(SMTestProxy.SMRootTestProxy testProxy, TestTreeRenderer renderer) {
        IconInfo iconInfo = getIcon(testProxy, renderer.getConsoleProperties());
        renderer.setIcon(iconInfo.icon());

        TestStateInfo.Magnitude magnitude = testProxy.getMagnitudeInfo();

        LocalizeValue text;
        String presentableName = testProxy.getPresentation();
        if (presentableName != null) {
            text = LocalizeValue.of(presentableName);
        }
        else if (magnitude == TestStateInfo.Magnitude.RUNNING_INDEX) {
            text = SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsRunningTests();
        }
        else if (magnitude == TestStateInfo.Magnitude.TERMINATED_INDEX) {
            text = SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsWasTerminated();
        }
        else {
            text = SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsTestResults();
        }
        renderer.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        String comment = testProxy.getComment();
        if (comment != null) {
            renderer.append(" (" + comment + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }

    public static void formatRootNodeWithoutChildren(SMTestProxy.SMRootTestProxy testProxy, TestTreeRenderer renderer) {
        TestStateInfo.Magnitude magnitude = testProxy.getMagnitudeInfo();
        if (magnitude == TestStateInfo.Magnitude.RUNNING_INDEX) {
            if (!testProxy.getChildren().isEmpty()) {
                formatRootNodeWithChildren(testProxy, renderer);
            }
            else {
                renderer.setIcon(getIcon(testProxy, renderer.getConsoleProperties()).icon());
                renderer.append(
                    SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsInstantiatingTests(),
                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                );
            }
        }
        else if (magnitude == TestStateInfo.Magnitude.NOT_RUN_INDEX) {
            renderer.setIcon(PoolOfTestIcons.NOT_RAN);
            renderer.append(
                SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsNotTestResults(),
                SimpleTextAttributes.ERROR_ATTRIBUTES
            );
        }
        else if (magnitude == TestStateInfo.Magnitude.TERMINATED_INDEX) {
            renderer.setIcon(PoolOfTestIcons.TERMINATED_ICON);
            renderer.append(
                SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsWasTerminated(),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            );
        }
        else if (magnitude == TestStateInfo.Magnitude.PASSED_INDEX) {
            renderer.setIcon(PoolOfTestIcons.PASSED_ICON);
            renderer.append(
                SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsAllTestsPassed(),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            );
        }
        else {
            if (!testProxy.getChildren().isEmpty()) {
                // some times test proxy may be updated faster than tests tree
                // so let's process such situation correctly
                formatRootNodeWithChildren(testProxy, renderer);
            }
            else {
                renderer.setIcon(PoolOfTestIcons.NOT_RAN);
                renderer.append(
                    testProxy.isTestsReporterAttached()
                        ? SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsNoTestsWereFound()
                        : SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsTestReporterNotAttached(),
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                );
            }
        }
    }

    public static void formatTestProxy(SMTestProxy testProxy, TestTreeRenderer renderer) {
        renderer.setIcon(getIcon(testProxy, renderer.getConsoleProperties()).icon());
        renderer.append(testProxy.getPresentableName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nonnull
    public static String getPresentableName(SMTestProxy testProxy) {
        SMTestProxy parent = testProxy.getParent();
        String name = testProxy.getName();

        if (name == null) {
            return SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsTestNoname().get();
        }

        String presentationCandidate = name;
        if (parent != null) {
            String parentName = parent.getName();
            if (parentName != null) {
                boolean parentStartsWith = name.startsWith(parentName);
                if (!parentStartsWith && parent instanceof SMTestProxy.SMRootTestProxy rootTestProxy) {
                    String presentation = rootTestProxy.getPresentation();
                    if (presentation != null) {
                        parentName = presentation;
                        parentStartsWith = name.startsWith(parentName);
                    }
                }
                if (parentStartsWith) {
                    presentationCandidate = name.substring(parentName.length());

                    // remove "." separator
                    presentationCandidate = StringUtil.trimStart(presentationCandidate, ".");
                }
            }
        }

        // trim
        presentationCandidate = presentationCandidate.trim();

        // remove extra spaces
        presentationCandidate = presentationCandidate.replaceAll("\\s+", " ");

        if (StringUtil.isEmpty(presentationCandidate)) {
            return SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsTestNoname().get();
        }

        return presentationCandidate;
    }

    @Nonnull
    public static String getPresentableNameTrimmedOnly(@Nonnull SMTestProxy testProxy) {
        String name = testProxy.getName();
        if (name != null) {
            name = name.trim();
        }
        if (name == null || name.isEmpty()) {
            name = SMTestLocalize.smTestRunnerUiTestsTreePresentationLabelsTestNoname().get();
        }
        return name;
    }

    @Nonnull
    private static IconInfo getIcon(SMTestProxy testProxy, TestConsoleProperties consoleProperties) {
        TestStateInfo.Magnitude magnitude = testProxy.getMagnitudeInfo();

        boolean hasErrors = testProxy.hasErrors();
        boolean hasPassedTests = testProxy.hasPassedTests();

        return switch (magnitude) {
            case ERROR_INDEX -> IconInfo.wrap(
                SMPoolOfTestIcons.ERROR_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusError().get()
            );
            case FAILED_INDEX -> hasErrors ? IconInfo.wrap(
                SMPoolOfTestIcons.FAILED_E_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusFailedWithErrors().get()
            )
                : IconInfo.wrap(
                SMPoolOfTestIcons.FAILED_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusFailed().get()
            );
            case IGNORED_INDEX -> hasErrors ? IconInfo.wrap(
                SMPoolOfTestIcons.IGNORED_E_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusIgnoredWithErrors().get()
            )
                : (hasPassedTests ? IconInfo.wrap(
                SMPoolOfTestIcons.PASSED_IGNORED,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusPassedWithIgnored().get()
            )
                : IconInfo.wrap(
                SMPoolOfTestIcons.IGNORED_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusIgnored().get()
            ));
            case NOT_RUN_INDEX -> IconInfo.wrap(
                SMPoolOfTestIcons.NOT_RAN,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusNotRan().get()
            );
            case COMPLETE_INDEX, PASSED_INDEX -> hasErrors ? IconInfo.wrap(
                SMPoolOfTestIcons.PASSED_E_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusPassedWithErrors().get()
            )
                : IconInfo.wrap(
                SMPoolOfTestIcons.PASSED_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusPassed().get()
            );
            case RUNNING_INDEX -> {
                if (consoleProperties.isPaused()) {
                    yield hasErrors
                        ? IconInfo.wrap(
                        SMPoolOfTestIcons.PAUSED_E_ICON,
                        SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusPausedWithErrors().get()
                    )
                        : IconInfo.wrap(
                        PlatformIconGroup.runconfigurationsTestpaused(),
                        SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusPaused().get()
                    );
                }
                else {
                    yield hasErrors ? IconInfo.wrap(
                        SMPoolOfTestIcons.RUNNING_E_ICON,
                        SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusRunningWithErrors().get()
                    )
                        : IconInfo.wrap(
                        SMPoolOfTestIcons.RUNNING_ICON,
                        SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusRunning().get()
                    );
                }
            }
            case SKIPPED_INDEX -> hasErrors ? IconInfo.wrap(
                SMPoolOfTestIcons.SKIPPED_E_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusSkippedWithErrors().get()
            )
                : IconInfo.wrap(
                SMPoolOfTestIcons.SKIPPED_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusSkipped().get()
            );
            case TERMINATED_INDEX -> hasErrors ? IconInfo.wrap(
                SMPoolOfTestIcons.TERMINATED_E_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusTerminatedWithErrors().get()
            )
                : IconInfo.wrap(
                SMPoolOfTestIcons.TERMINATED_ICON,
                SMTestLocalize.smTestRunnerUiTestsTreePresentationAccessibleStatusTerminated().get()
            );
        };
    }

    @Nullable
    public static String getTestStatusPresentation(SMTestProxy proxy) {
        return proxy.getMagnitudeInfo().getTitle();
    }

    public static void appendSuiteStatusColorPresentation(SMTestProxy proxy, ColoredTableCellRenderer renderer) {
        int passedCount = 0;
        int errorsCount = 0;
        int failedCount = 0;
        int ignoredCount = 0;

        if (proxy.isLeaf()) {
            // If suite is empty show <no tests> label and exit from method
            renderer.append(
                SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsNoTests(),
                proxy.wasLaunched() ? PASSED_ATTRIBUTES : DEFFECT_ATTRIBUTES
            );
            return;
        }

        List<SMTestProxy> allTestCases = proxy.getAllTests();
        for (SMTestProxy testOrSuite : allTestCases) {
            // we should ignore test suites
            if (testOrSuite.isSuite()) {
                continue;
            }
            // if test check it state
            switch (testOrSuite.getMagnitudeInfo()) {
                case COMPLETE_INDEX:
                case PASSED_INDEX:
                    passedCount++;
                    break;
                case ERROR_INDEX:
                    errorsCount++;
                    break;
                case FAILED_INDEX:
                    failedCount++;
                    break;
                case IGNORED_INDEX:
                case SKIPPED_INDEX:
                    ignoredCount++;
                    break;
                case NOT_RUN_INDEX:
                case TERMINATED_INDEX:
                case RUNNING_INDEX:
                    //Do nothing
                    break;
            }
        }

        if (failedCount > 0) {
            renderer.append(
                LocalizeValue.join(
                    SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsCountMsgFailed(failedCount),
                    LocalizeValue.space()
                ),
                DEFFECT_ATTRIBUTES
            );
        }

        if (errorsCount > 0) {
            renderer.append(
                LocalizeValue.join(
                    SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsCountMsgErrors(errorsCount),
                    LocalizeValue.space()
                ),
                DEFFECT_ATTRIBUTES
            );
        }

        if (ignoredCount > 0) {
            renderer.append(
                LocalizeValue.join(
                    SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsCountMsgIgnored(ignoredCount),
                    LocalizeValue.space()
                ),
                SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
            );
        }

        if (passedCount > 0) {
            renderer.append(
                SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsCountMsgPassed(passedCount),
                PASSED_ATTRIBUTES
            );
        }
    }

    /**
     * @param proxy Test or Suite
     * @return Duration presentation for given proxy
     */
    @Nullable
    public static String getDurationPresentation(SMTestProxy proxy) {
        return switch (proxy.getMagnitudeInfo()) {
            case COMPLETE_INDEX, PASSED_INDEX, FAILED_INDEX, ERROR_INDEX, IGNORED_INDEX, SKIPPED_INDEX ->
                getDurationTimePresentation(proxy);

            case NOT_RUN_INDEX -> SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationNotRun().get();

            case RUNNING_INDEX -> getDurationWithPrefixPresentation(
                proxy,
                SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationPrefixRunning().get()
            );

            case TERMINATED_INDEX -> getDurationWithPrefixPresentation(
                proxy,
                SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationPrefixTerminated().get()
            );

            default -> SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationUnknown().get();
        };
    }

    private static String getDurationWithPrefixPresentation(SMTestProxy proxy, String prefix) {
        // If duration is known
        if (proxy.getDuration() != null) {
            return prefix + COLON + getDurationTimePresentation(proxy);
        }

        return '<' + prefix + '>';
    }

    private static String getDurationTimePresentation(SMTestProxy proxy) {
        Long duration = proxy.getDuration();

        if (duration == null) {
            // if suite without children
            return proxy.isSuite() && proxy.isLeaf()
                ? SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationNoTests().get()
                : SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationUnknown().get();
        }
        else {
            return StringUtil.formatDuration(duration);
        }
    }

    public static void appendTestStatusColorPresentation(SMTestProxy proxy, ColoredTableCellRenderer renderer) {
        String title = getTestStatusPresentation(proxy);

        TestStateInfo.Magnitude info = proxy.getMagnitudeInfo();
        switch (info) {
            case COMPLETE_INDEX, PASSED_INDEX -> renderer.append(title, PASSED_ATTRIBUTES);
            case RUNNING_INDEX -> renderer.append(title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            case NOT_RUN_INDEX -> renderer.append(title, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            case IGNORED_INDEX, SKIPPED_INDEX -> renderer.append(title, SimpleTextAttributes.EXCLUDED_ATTRIBUTES);
            case ERROR_INDEX, FAILED_INDEX -> renderer.append(title, DEFFECT_ATTRIBUTES);
            case TERMINATED_INDEX -> renderer.append(title, TERMINATED_ATTRIBUTES);
        }
    }

    public static void printWithAnsiColoring(@Nonnull Printer printer, @Nonnull String text, @Nonnull Key processOutputType) {
        AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();
        decoder.escapeText(
            text,
            ProcessOutputTypes.STDOUT,
            (text1, attributes) -> {
                ConsoleViewContentType contentType = ConsoleViewContentType.getConsoleViewType(attributes);
                if (contentType == null || contentType == ConsoleViewContentType.NORMAL_OUTPUT) {
                    contentType = ConsoleViewContentType.getConsoleViewType(processOutputType);
                }
                printer.print(text1, contentType);
            }
        );
    }

    private static record IconInfo(Image icon, String statusText) {
        @Deprecated
        private Image getIcon() {
            return icon();
        }

        @Deprecated
        private String getStatusText() {
            return statusText();
        }

        static IconInfo wrap(Image icon, String statusText) {
            return new IconInfo(icon, statusText);
        }
    }
}
