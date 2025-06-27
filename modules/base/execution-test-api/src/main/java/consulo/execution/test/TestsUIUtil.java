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
package consulo.execution.test;

import consulo.dataContext.DataContext;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.localize.ExecutionLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.AppIconScheme;
import consulo.ui.ex.SystemNotifications;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

public class TestsUIUtil {
    public static final NotificationGroup NOTIFICATION_GROUP =
        NotificationGroup.logOnlyGroup("testRunner", LocalizeValue.localizeTODO("Test Runner"));

    public static final Color PASSED_COLOR = new Color(0, 128, 0);
    private static final String TESTS = "tests";

    private TestsUIUtil() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getData(AbstractTestProxy testProxy, Key<T> dataId, TestFrameworkRunningModel model) {
        TestConsoleProperties properties = model.getProperties();
        Project project = properties.getProject();
        if (testProxy == null) {
            return null;
        }
        if (AbstractTestProxy.KEY == dataId) {
            return (T) testProxy;
        }
        if (Navigatable.KEY == dataId) {
            return (T) getOpenFileDescriptor(testProxy, model);
        }
        if (PsiElement.KEY == dataId) {
            Location location = testProxy.getLocation(project, properties.getScope());
            if (location != null) {
                PsiElement element = location.getPsiElement();
                return element.isValid() ? (T) element : null;
            }
            else {
                return null;
            }
        }
        if (Location.DATA_KEY == dataId) {
            return (T) testProxy.getLocation(project, properties.getScope());
        }
        if (RunConfiguration.KEY == dataId && properties.getConfiguration() instanceof RunConfiguration runConfiguration) {
            return (T) runConfiguration;
        }
        return null;
    }

    public static boolean isMultipleSelectionImpossible(DataContext dataContext) {
        Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (component instanceof JTree tree) {
            TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths == null || selectionPaths.length <= 1) {
                return true;
            }
        }
        return false;
    }

    public static Navigatable getOpenFileDescriptor(AbstractTestProxy testProxy, TestFrameworkRunningModel model) {
        TestConsoleProperties testConsoleProperties = model.getProperties();
        return getOpenFileDescriptor(
            testProxy,
            testConsoleProperties,
            TestConsoleProperties.OPEN_FAILURE_LINE.value(testConsoleProperties)
        );
    }

    private static Navigatable getOpenFileDescriptor(
        AbstractTestProxy proxy,
        TestConsoleProperties testConsoleProperties,
        boolean openFailureLine
    ) {
        Project project = testConsoleProperties.getProject();

        if (proxy != null) {
            Location location = proxy.getLocation(project, testConsoleProperties.getScope());
            if (openFailureLine) {
                return proxy.getDescriptor(location, testConsoleProperties);
            }
            OpenFileDescriptor openFileDescriptor = location == null ? null : location.getOpenFileDescriptor();
            if (openFileDescriptor != null && openFileDescriptor.getFile().isValid()) {
                return openFileDescriptor;
            }
        }
        return null;
    }

    @RequiredUIAccess
    public static void notifyByBalloon(
        @Nonnull Project project,
        boolean started,
        AbstractTestProxy root,
        TestConsoleProperties properties,
        @Nullable String comment
    ) {
        notifyByBalloon(project, root, properties, new TestResultPresentation(root, started, comment).getPresentation());
    }

    @RequiredUIAccess
    public static void notifyByBalloon(
        @Nonnull Project project,
        AbstractTestProxy root,
        TestConsoleProperties properties,
        TestResultPresentation testResultPresentation
    ) {
        if (project.isDisposed()) {
            return;
        }
        if (properties == null) {
            return;
        }

        TestStatusListener.notifySuiteFinished(root, properties.getProject());

        String testRunDebugId = properties.isDebug() ? ToolWindowId.DEBUG : ToolWindowId.RUN;
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

        String title = testResultPresentation.getTitle();
        String text = testResultPresentation.getText();
        String balloonText = testResultPresentation.getBalloonText();
        NotificationType type = testResultPresentation.getType();

        if (!Comparing.strEqual(toolWindowManager.getActiveToolWindowId(), testRunDebugId)) {
            toolWindowManager.notifyByBalloon(testRunDebugId, type.toUI(), balloonText);
        }

        NotificationService.getInstance()
            .newOfType(NOTIFICATION_GROUP, type)
            .content(LocalizeValue.localizeTODO(balloonText))
            .notify(project);
        SystemNotifications.getInstance().notify("TestRunner", title, text);
    }

    public static String getTestSummary(AbstractTestProxy proxy) {
        return new TestResultPresentation(proxy).getPresentation().getBalloonText();
    }

    public static String getTestShortSummary(AbstractTestProxy proxy) {
        return new TestResultPresentation(proxy).getPresentation().getText();
    }

    public static void showIconProgress(Project project, int n, int maximum, int problemsCounter, boolean updateWithAttention) {
        AppIcon icon = AppIcon.getInstance();
        if (n < maximum || !updateWithAttention) {
            if (!updateWithAttention || icon.setProgress(
                project,
                TESTS,
                AppIconScheme.Progress.TESTS,
                (double) n / (double) maximum,
                problemsCounter == 0
            )) {
                if (problemsCounter > 0) {
                    icon.setErrorBadge(project, String.valueOf(problemsCounter));
                }
            }
        }
        else {
            if (icon.hideProgress(project, TESTS)) {
                if (problemsCounter > 0) {
                    icon.setErrorBadge(project, String.valueOf(problemsCounter));
                    icon.requestAttention(project, false);
                }
                else {
                    icon.setOkBadge(project, true);
                    icon.requestAttention(project, false);
                }
            }
        }
    }

    public static void clearIconProgress(Project project) {
        AppIcon.getInstance().hideProgress(project, TESTS);
        AppIcon.getInstance().setErrorBadge(project, null);
    }

    public static class TestResultPresentation {
        private AbstractTestProxy myRoot;
        private boolean myStarted;
        private String myComment;
        private String myTitle;
        private String myText;
        private String myBalloonText;
        private NotificationType myType;

        public TestResultPresentation(AbstractTestProxy root, boolean started, String comment) {
            myRoot = root;
            myStarted = started;
            myComment = comment;
        }

        public TestResultPresentation(AbstractTestProxy root) {
            this(root, true, null);
        }

        public String getTitle() {
            return myTitle;
        }

        public String getText() {
            return myText;
        }

        public String getBalloonText() {
            return myBalloonText;
        }

        public NotificationType getType() {
            return myType;
        }

        public TestResultPresentation getPresentation() {
            List<? extends AbstractTestProxy> allTests = Filter.LEAF.select(myRoot.getAllTests());
            List<AbstractTestProxy> failed = Filter.DEFECTIVE_LEAF.select(allTests);
            List<AbstractTestProxy> notStarted = Filter.NOT_PASSED.select(allTests);
            notStarted.removeAll(failed);
            List<? extends AbstractTestProxy> ignored = Filter.IGNORED.select(allTests);
            notStarted.removeAll(ignored);
            failed.removeAll(ignored);
            int failedCount = failed.size();
            int notStartedCount = notStarted.size() + ignored.size();
            int passedCount = allTests.size() - failedCount - notStartedCount;
            return getPresentation(failedCount, passedCount, notStartedCount, ignored.size());
        }

        public TestResultPresentation getPresentation(int failedCount, int passedCount, int notStartedCount, int ignoredCount) {
            if (myRoot == null) {
                myBalloonText = myTitle = myStarted ? "Tests were interrupted" : ExecutionLocalize.testNotStartedProgressText().get();
                myText = "";
                myType = NotificationType.WARNING;
            }
            else {
                if (failedCount > 0) {
                    myTitle = ExecutionLocalize.junitRuningInfoTestsFailedLabel().get();
                    myText =
                        passedCount + " passed, " + failedCount + " failed" + (notStartedCount > 0 ? ", " + notStartedCount + " not started" : "");
                    myType = NotificationType.ERROR;
                }
                else if (notStartedCount > 0) {
                    myTitle = ignoredCount > 0 ? "Tests Ignored" : ExecutionLocalize.junitRunningInfoFailedToStartErrorMessage().get();
                    myText = passedCount + " passed, " + notStartedCount + (ignoredCount > 0 ? " ignored" : " not started");
                    myType = ignoredCount == 0 ? NotificationType.WARNING : NotificationType.ERROR;
                }
                else {
                    myTitle = ExecutionLocalize.junitRuningInfoTestsPassedLabel().get();
                    myText = passedCount + " passed";
                    myType = NotificationType.INFORMATION;
                }
                if (myComment != null) {
                    myText += " " + myComment;
                }
                myBalloonText = myTitle + ": " + myText;
            }
            return this;
        }

    }
}
