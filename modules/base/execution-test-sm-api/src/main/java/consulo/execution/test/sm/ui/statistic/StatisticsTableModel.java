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

import consulo.execution.test.sm.SMRunnerUtil;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class StatisticsTableModel extends ListTableModel<SMTestProxy> {
    private static final Logger LOG = Logger.getInstance(StatisticsTableModel.class);

    private SMTestProxy myCurrentSuite;

    public StatisticsTableModel() {
        super(new ColumnTest(), new ColumnDuration(), new ColumnResults());
        setSortable(false); // TODO: fix me
    }

    public void updateModelOnProxySelected(SMTestProxy proxy) {
        SMTestProxy newCurrentSuite = getCurrentSuiteFor(proxy);
        // If new suite differs from old suite we should reload table
        if (myCurrentSuite != newCurrentSuite) {
            myCurrentSuite = newCurrentSuite;
        }
        // update model to show new items in it
        SMRunnerUtil.addToInvokeLater(this::updateModel);
    }

    @Nullable
    public SMTestProxy getTestAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex > getItems().size()) {
            return null;
        }
        return getItems().get(rowIndex);
    }


    /**
     * Searches index of given test or suite. If finds nothing will retun -1
     *
     * @param test Test or suite
     * @return Proxy's index or -1
     */
    public int getIndexOf(SMTestProxy test) {
        for (int i = 0; i < getItems().size(); i++) {
            SMTestProxy child = getItems().get(i);
            if (child == test) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Update module in EDT
     */
    protected void updateModel() {
        LOG.assertTrue(SwingUtilities.isEventDispatchThread());

        // updates model
        setItems(getItemsForSuite(myCurrentSuite));
    }

    @Nonnull
    private List<SMTestProxy> getItemsForSuite(@Nullable SMTestProxy suite) {
        if (suite == null) {
            return Collections.emptyList();
        }

        List<SMTestProxy> list = new ArrayList<>();
        // suite's total statistics
        list.add(suite);
        // chiled's statistics
        list.addAll(suite.getChildren());

        return list;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Setting value is prevented!
        LOG.error("value: " + aValue + " row: " + rowIndex + " column: " + columnIndex);
    }

    @Nullable
    private SMTestProxy getCurrentSuiteFor(@Nullable SMTestProxy proxy) {
        if (proxy == null) {
            return null;
        }

        // If proxy is suite, returns it
        SMTestProxy suite;
        if (proxy.isSuite()) {
            suite = proxy;
        }
        else {
            // If proxy is tests returns test's suite
            suite = proxy.getParent();
        }
        return suite;
    }

    protected boolean shouldUpdateModelByTest(SMTestProxy test) {
        // if some suite in statistics is selected
        // and test is child of current suite
        return isSomeSuiteSelected() && (test.getParent() == myCurrentSuite);
    }

    protected boolean shouldUpdateModelBySuite(SMTestProxy suite) {
        // If some suite in statistics is selected
        // and suite is current suite in statistics tab or child of current suite
        return isSomeSuiteSelected() && (suite == myCurrentSuite || suite.getParent() == myCurrentSuite);
    }

    private boolean isSomeSuiteSelected() {
        return myCurrentSuite != null;
    }
}
