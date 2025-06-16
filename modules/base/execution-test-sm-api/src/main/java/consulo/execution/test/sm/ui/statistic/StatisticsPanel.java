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
package consulo.execution.test.sm.ui.statistic;

import consulo.application.ApplicationPropertiesComponent;
import consulo.component.util.config.Storage;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.TestsUIUtil;
import consulo.execution.test.sm.SMRunnerUtil;
import consulo.execution.test.sm.runner.SMTRunnerEventsAdapter;
import consulo.execution.test.sm.runner.SMTRunnerEventsListener;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.sm.ui.PropagateSelectionHandler;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.table.BaseTableView;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class StatisticsPanel implements DataProvider {
    public static final Key<StatisticsPanel> SM_TEST_RUNNER_STATISTICS = Key.create("SM_TEST_RUNNER_STATISTICS");

    private TableView<SMTestProxy> myStatisticsTableView;
    private JPanel myContentPane;
    private final Storage.PropertiesComponentStorage myStorage =
        new Storage.PropertiesComponentStorage("sm_test_statistics_table_columns", ApplicationPropertiesComponent.getInstance());

    private final StatisticsTableModel myTableModel;
    private final List<PropagateSelectionHandler> myPropagateSelectionHandlers = Lists.newLockFreeCopyOnWriteList();
    private final Project myProject;
    private final TestFrameworkRunningModel myFrameworkRunningModel;

    public StatisticsPanel(Project project, TestFrameworkRunningModel model) {
        myProject = project;
        myTableModel = new StatisticsTableModel();
        myStatisticsTableView.setModelAndUpdateColumns(myTableModel);
        myFrameworkRunningModel = model;

        final Runnable gotoSuiteOrParentAction = createGotoSuiteOrParentAction();
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                gotoSuiteOrParentAction.run();
                return true;
            }
        }.installOn(myStatisticsTableView);

        // Fire selection changed and move focus on SHIFT+ENTER
        KeyStroke shiftEnterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK);
        SMRunnerUtil.registerAsAction(
            shiftEnterKey,
            "select-test-proxy-in-test-view",
            this::showSelectedProxyInTestsTree,
            myStatisticsTableView
        );

        // Expand selected or go to parent on ENTER
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        SMRunnerUtil.registerAsAction(enterKey, "go-to-selected-suite-or-parent", gotoSuiteOrParentAction, myStatisticsTableView);
        // Contex menu in Table
        PopupHandler.installPopupHandler(myStatisticsTableView, IdeActions.GROUP_TESTTREE_POPUP, ActionPlaces.TESTTREE_VIEW_POPUP);
        // set this statistic tab as dataprovider for test's table view
        DataManager.registerDataProvider(myStatisticsTableView, this);
    }

    public void addPropagateSelectionListener(PropagateSelectionHandler handler) {
        myPropagateSelectionHandlers.add(handler);
    }

    public JPanel getContentPane() {
        return myContentPane;
    }

    public SMTRunnerEventsListener createTestEventsListener() {
        return new SMTRunnerEventsAdapter() {
            @Override
            public void onSuiteStarted(@Nonnull SMTestProxy suite) {
                if (myTableModel.shouldUpdateModelBySuite(suite)) {
                    updateAndRestoreSelection();
                }
            }

            @Override
            public void onSuiteFinished(@Nonnull SMTestProxy suite) {
                if (myTableModel.shouldUpdateModelBySuite(suite)) {
                    updateAndRestoreSelection();
                }
            }

            @Override
            public void onTestStarted(@Nonnull SMTestProxy test) {
                if (myTableModel.shouldUpdateModelByTest(test)) {
                    updateAndRestoreSelection();
                }
            }

            @Override
            public void onTestFinished(@Nonnull SMTestProxy test) {
                if (myTableModel.shouldUpdateModelByTest(test)) {
                    updateAndRestoreSelection();
                }
            }

            private void updateAndRestoreSelection() {
                SMRunnerUtil.addToInvokeLater(() -> {
                    BaseTableView.restore(myStorage, myStatisticsTableView);
                    // statisticsTableView can be null in JUnit tests
                    SMTestProxy oldSelection = myStatisticsTableView.getSelectedObject();

                    // update module
                    myTableModel.updateModel();

                    // restore selection if it is possible
                    if (oldSelection != null) {
                        int newRow = myTableModel.getIndexOf(oldSelection);
                        if (newRow > -1) {
                            myStatisticsTableView.setRowSelectionInterval(newRow, newRow);
                        }
                    }
                });
            }
        };
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (SM_TEST_RUNNER_STATISTICS == dataId) {
            return this;
        }
        return TestsUIUtil.getData(getSelectedItem(), dataId, myFrameworkRunningModel);
    }

    /**
     * On event - change selection and probably requests focus. Is used when we want
     * navigate from other component to this
     *
     * @return Listener
     */
    public PropagateSelectionHandler createSelectMeListener() {
        return this::selectProxy;
    }

    public void selectProxy(@Nullable final SMTestProxy selectedTestProxy, @Nonnull Object sender, final boolean requestFocus) {
        SMRunnerUtil.addToInvokeLater(() -> {
            // Select tab if focus was requested
            if (requestFocus) {
                ProjectIdeFocusManager.getInstance(myProject).requestFocus(myStatisticsTableView, true);
            }

            // Select proxy in table
            selectProxy(selectedTestProxy);
        });
    }

    public void showSelectedProxyInTestsTree() {
        Collection<SMTestProxy> proxies = myStatisticsTableView.getSelection();
        if (proxies.isEmpty()) {
            return;
        }
        SMTestProxy proxy = proxies.iterator().next();
        myStatisticsTableView.clearSelection();
        fireOnPropagateSelection(proxy);
    }

    protected Runnable createGotoSuiteOrParentAction() {
        // Expand selected or go to parent
        return () -> {
            SMTestProxy selectedProxy = getSelectedItem();
            if (selectedProxy == null) {
                return;
            }

            int i = myStatisticsTableView.getSelectedRow();
            assert i >= 0; //because something is selected

            // If first line is selected we should go to parent suite
            if (ColumnTest.TestsCellRenderer.isFirstLine(i)) {
                SMTestProxy parentSuite = selectedProxy.getParent();
                if (parentSuite != null) {
                    // go to parent and current suit in it
                    showInTableAndSelectRow(parentSuite, selectedProxy);
                }
            }
            else {
                // if selected element is suite - we should expand it
                if (selectedProxy.isSuite()) {
                    // expand and select first (Total) row
                    showInTableAndSelectRow(selectedProxy, selectedProxy);
                }
            }
        };
    }

    protected void selectProxy(@Nullable SMTestProxy selectedTestProxy) {
        // Send event to model
        myTableModel.updateModelOnProxySelected(selectedTestProxy);

        // Now we want to select proxy in table (if it is possible)
        if (selectedTestProxy != null) {
            findAndSelectInTable(selectedTestProxy);
        }
    }

    /**
     * Selects row in table
     *
     * @param rowIndex Row's index
     */
    protected void selectRow(int rowIndex) {
        SMRunnerUtil.addToInvokeLater(() -> {
            // updates model
            myStatisticsTableView.setRowSelectionInterval(rowIndex, rowIndex);

            // Scroll to visible
            TableUtil.scrollSelectionToVisible(myStatisticsTableView);
        });
    }

    /**
     * Selects row in table
     */
    protected void selectRowOf(SMTestProxy proxy) {
        SMRunnerUtil.addToInvokeLater(() -> {
            int rowIndex = myTableModel.getIndexOf(proxy);
            myStatisticsTableView.setRowSelectionInterval(rowIndex, rowIndex >= 0 ? rowIndex : 0);
            // Scroll to visible
            TableUtil.scrollSelectionToVisible(myStatisticsTableView);
        });
    }

    @Nullable
    protected SMTestProxy getSelectedItem() {
        return myStatisticsTableView.getSelectedObject();
    }

    protected List<SMTestProxy> getTableItems() {
        return myTableModel.getItems();
    }

    private void findAndSelectInTable(SMTestProxy proxy) {
        SMRunnerUtil.addToInvokeLater(() -> {
            int rowIndex = myTableModel.getIndexOf(proxy);
            if (rowIndex >= 0) {
                myStatisticsTableView.setRowSelectionInterval(rowIndex, rowIndex);
            }
        });
    }

    private void fireOnPropagateSelection(SMTestProxy selectedTestProxy) {
        for (PropagateSelectionHandler handler : myPropagateSelectionHandlers) {
            handler.handlePropagateSelectionRequest(selectedTestProxy, this, true);
        }
    }

    private void createUIComponents() {
        myStatisticsTableView = new TableView<>();
    }

    private void showInTableAndSelectRow(SMTestProxy suite, SMTestProxy suiteProxy) {
        selectProxy(suite);
        selectRowOf(suiteProxy);
    }

    public void doDispose() {
        BaseTableView.store(myStorage, myStatisticsTableView);
    }
}
