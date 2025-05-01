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
package consulo.execution.test.ui;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.action.DumbAwareToggleBooleanProperty;
import consulo.application.ui.action.DumbAwareToggleInvertedBooleanProperty;
import consulo.application.ui.action.ToggleBooleanProperty;
import consulo.disposer.Disposable;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.FailedTestsNavigator;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.action.*;
import consulo.execution.test.export.ExportTestResultsAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author anna
 * @since 2007-05-25
 */
public class ToolbarPanel extends JPanel implements OccurenceNavigator, Disposable {
    protected final TestTreeExpander myTreeExpander = new TestTreeExpander();
    protected final FailedTestsNavigator myOccurenceNavigator;
    protected final ScrollToTestSourceAction myScrollToSource;
    @Nullable
    private ExportTestResultsAction myExportAction;

    private final ArrayList<ToggleModelAction> myActions = new ArrayList<>();

    public ToolbarPanel(TestConsoleProperties properties, JComponent parent) {
        super(new BorderLayout());
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.addAction(new DumbAwareToggleInvertedBooleanProperty(
            ExecutionLocalize.junitRunHidePassedActionName().get(),
            ExecutionLocalize.junitRunHidePassedActionDescription().get(),
            PlatformIconGroup.runconfigurationsTestpassed(),
            properties,
            TestConsoleProperties.HIDE_PASSED_TESTS
        ));
        actionGroup.add(new DumbAwareToggleInvertedBooleanProperty(
            "Show Ignored",
            "Show Ignored",
            PlatformIconGroup.runconfigurationsTestignored(),
            properties,
            TestConsoleProperties.HIDE_IGNORED_TEST
        ));
        actionGroup.addSeparator();

        actionGroup.addAction(new DumbAwareToggleBooleanProperty(
            ExecutionLocalize.junitRuningInfoSortAlphabeticallyActionName().get(),
            ExecutionLocalize.junitRuningInfoSortAlphabeticallyActionDescription().get(),
            PlatformIconGroup.objectbrowserSorted(),
            properties,
            TestConsoleProperties.SORT_ALPHABETICALLY
        ));
        ToggleModelAction sortByStatistics = new SortByDurationAction(properties);
        myActions.add(sortByStatistics);
        actionGroup.addAction(sortByStatistics);
        actionGroup.addSeparator();

        AnAction action = CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, parent);
        action.getTemplatePresentation().setDescriptionValue(ExecutionLocalize.junitRuningInfoExpandTestActionName());
        actionGroup.add(action);

        action = CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, parent);
        action.getTemplatePresentation().setDescriptionValue(ExecutionLocalize.junitRuningInfoCollapseTestActionName());
        actionGroup.add(action);

        actionGroup.addSeparator();
        CommonActionsManager actionsManager = CommonActionsManager.getInstance();
        myOccurenceNavigator = new FailedTestsNavigator();
        actionGroup.add(actionsManager.createPrevOccurenceAction(myOccurenceNavigator));
        actionGroup.add(actionsManager.createNextOccurenceAction(myOccurenceNavigator));

        Application.get().getExtensionPoint(ToggleModelActionProvider.class).forEach(actionProvider -> {
            ToggleModelAction toggleModelAction = actionProvider.createToggleModelAction(properties);
            myActions.add(toggleModelAction);
            actionGroup.add(toggleModelAction);
        });

        RunProfile configuration = properties.getConfiguration();
        if (configuration instanceof RunConfiguration runConfiguration) {
            myExportAction = ExportTestResultsAction.create(properties.getExecutor().getToolWindowId(), runConfiguration);
            actionGroup.addAction(myExportAction);
        }

        AnAction importAction = properties.createImportAction();
        if (importAction != null) {
            actionGroup.addAction(importAction);
        }

        DefaultActionGroup secondaryGroup = new DefaultActionGroup();
        secondaryGroup.setPopup(true);
        secondaryGroup.getTemplatePresentation().setIcon(PlatformIconGroup.generalGearplain());
        secondaryGroup.add(new DumbAwareToggleBooleanProperty(
            ExecutionLocalize.junitRuningInfoTrackTestActionName().get(),
            ExecutionLocalize.junitRuningInfoTrackTestActionDescription().get(),
            null,
            properties,
            TestConsoleProperties.TRACK_RUNNING_TEST
        ));

        secondaryGroup.add(new DumbAwareToggleBooleanProperty(
            "Show Inline Statistics",
            "Toggle the visibility of the test duration in the tree",
            null,
            properties,
            TestConsoleProperties.SHOW_INLINE_STATISTICS
        ));

        secondaryGroup.addSeparator();
        secondaryGroup.add(new DumbAwareToggleBooleanProperty(
            ExecutionLocalize.junitRuningInfoScrollToStacktraceActionName().get(),
            ExecutionLocalize.junitRuningInfoScrollToStacktraceActionDescription().get(),
            null,
            properties,
            TestConsoleProperties.SCROLL_TO_STACK_TRACE
        ));
        secondaryGroup.add(new ToggleBooleanProperty(
            ExecutionLocalize.junitRuningInfoOpenSourceAtExceptionActionName().get(),
            ExecutionLocalize.junitRuningInfoOpenSourceAtExceptionActionDescription().get(),
            null,
            properties,
            TestConsoleProperties.OPEN_FAILURE_LINE
        ));
        myScrollToSource = new ScrollToTestSourceAction(properties);
        secondaryGroup.add(myScrollToSource);

        secondaryGroup.add(new AdjustAutotestDelayActionGroup(parent));
        secondaryGroup.addSeparator();
        secondaryGroup.add(new DumbAwareToggleBooleanProperty(
            ExecutionLocalize.junitRuningInfoSelectFirstFailedActionName().get(),
            null,
            null,
            properties,
            TestConsoleProperties.SELECT_FIRST_DEFECT
        ));
        properties.appendAdditionalActions(secondaryGroup, parent, properties);
        actionGroup.add(secondaryGroup);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TESTTREE_VIEW_TOOLBAR, actionGroup, true);
        toolbar.setTargetComponent(this);
        add(toolbar.getComponent(), BorderLayout.CENTER);
    }

    public void setModel(TestFrameworkRunningModel model) {
        TestFrameworkActions.installFilterAction(model);
        myScrollToSource.setModel(model);
        myTreeExpander.setModel(model);
        myOccurenceNavigator.setModel(model);
        if (myExportAction != null) {
            myExportAction.setModel(model);
        }
        for (ToggleModelAction action : myActions) {
            action.setModel(model);
        }
        TestFrameworkActions.addPropertyListener(TestConsoleProperties.SORT_ALPHABETICALLY, value -> {
            AbstractTestTreeBuilder builder = model.getTreeBuilder();
            if (builder != null) {
                builder.setTestsComparator(value);
            }
        }, model, true);
        TestFrameworkActions.addPropertyListener(TestConsoleProperties.SORT_BY_DURATION, value -> {
            AbstractTestTreeBuilder builder = model.getTreeBuilder();
            if (builder != null) {
                builder.setStatisticsComparator(model.getProperties(), value);
            }
        }, model, true);
    }

    @Override
    public boolean hasNextOccurence() {
        return myOccurenceNavigator.hasNextOccurence();
    }

    @Override
    public boolean hasPreviousOccurence() {
        return myOccurenceNavigator.hasPreviousOccurence();
    }

    @Override
    public OccurenceInfo goNextOccurence() {
        return myOccurenceNavigator.goNextOccurence();
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
        return myOccurenceNavigator.goPreviousOccurence();
    }

    @Override
    public String getNextOccurenceActionName() {
        return myOccurenceNavigator.getNextOccurenceActionName();
    }

    @Override
    public String getPreviousOccurenceActionName() {
        return myOccurenceNavigator.getPreviousOccurenceActionName();
    }

    @Override
    public void dispose() {
        myScrollToSource.setModel(null);
        if (myExportAction != null) {
            myExportAction.setModel(null);
        }
    }

    private static class SortByDurationAction extends ToggleModelAction implements DumbAware {

        private TestFrameworkRunningModel myModel;

        public SortByDurationAction(TestConsoleProperties properties) {
            super(
                ExecutionLocalize.junitRuningInfoSortByStatisticsActionName().get(),
                ExecutionLocalize.junitRuningInfoSortByStatisticsActionDescription().get(),
                PlatformIconGroup.runconfigurationsSortbyduration(),
                properties,
                TestConsoleProperties.SORT_BY_DURATION
            );
        }

        @Override
        protected boolean isEnabled() {
            TestFrameworkRunningModel model = myModel;
            return model != null && !model.isRunning();
        }

        @Override
        public void setModel(TestFrameworkRunningModel model) {
            myModel = model;
        }
    }
}
