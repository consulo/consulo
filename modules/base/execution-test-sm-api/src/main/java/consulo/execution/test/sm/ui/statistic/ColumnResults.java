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

import consulo.execution.test.sm.localize.SMTestLocalize;
import consulo.execution.test.sm.runner.ProxyFilters;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.sm.ui.TestsPresentationUtil;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.util.lang.ComparatorUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;

/**
 * @author Roman Chernyatchik
 */
public class ColumnResults extends BaseColumn implements Comparator<SMTestProxy> {
    public ColumnResults() {
        super(SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsTitle());
    }

    @Override
    public Comparator<SMTestProxy> getComparator() {
        return this;
    }

    @Override
    public int compare(SMTestProxy proxy1, SMTestProxy proxy2) {
        // Rule0. Test < Suite
        // Rule1. For tests: NotRun < Ignored, etc < Passed < Failure < Error < Progress < Terminated
        // Rule2. For suites: Checks count of passed, failures and errors tests: passed < failures < errors

        if (proxy1.isSuite()) {
            if (proxy2.isSuite()) {
                //proxy1 - suite
                //proxy2 - suite

                return compareSuites(proxy1, proxy2);
            }
            else {
                //proxy1 - suite
                //proxy2 - test
                return +1;
            }
        }
        else {
            if (proxy2.isSuite()) {
                //proxy1 - test
                //proxy2 - suite
                return -1;
            }
            else {
                //proxy1 - test
                //proxy2 - test
                return compareTests(proxy1, proxy2);
            }
        }
    }

    private int compareTests(SMTestProxy test1, SMTestProxy test2) {
        // Rule1. For tests: NotRun < Ignored, etc < Passed < Failure < Error < Progress < Terminated

        int weight1 = test1.getMagnitudeInfo().getSortWeight();
        int weight2 = test2.getMagnitudeInfo().getSortWeight();

        return ComparatorUtil.compareInt(weight1, weight2);
    }

    private int compareSuites(SMTestProxy suite1, SMTestProxy suite2) {
        // Compare errors
        int errors1 = suite1.getChildren(ProxyFilters.FILTER_ERRORS).size();
        int errors2 = suite2.getChildren(ProxyFilters.FILTER_ERRORS).size();
        int errorsComparison = ComparatorUtil.compareInt(errors1, errors2);
        // If not equal return it
        if (errorsComparison != 0) {
            return errorsComparison;
        }

        // Compare failures
        int failures1 = suite1.getChildren(ProxyFilters.FILTER_FAILURES).size();
        int failures2 = suite2.getChildren(ProxyFilters.FILTER_FAILURES).size();
        int failuresComparison = ComparatorUtil.compareInt(failures1, failures2);
        // If not equal return it
        if (failuresComparison != 0) {
            return failuresComparison;
        }

        // otherwise check passed count
        int passed1 = suite1.getChildren(ProxyFilters.FILTER_PASSED).size();
        int passed2 = suite2.getChildren(ProxyFilters.FILTER_PASSED).size();

        return ComparatorUtil.compareInt(passed1, passed2);
    }

    @Override
    public String valueOf(SMTestProxy testProxy) {
        return SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsResultsUndefined().get();
    }

    @Override
    public TableCellRenderer getRenderer(SMTestProxy proxy) {
        return new ResultsCellRenderer(proxy);
    }

    public static class ResultsCellRenderer extends ColoredTableCellRenderer implements ColoredRenderer {
        private final SMTestProxy myProxy;

        public ResultsCellRenderer(SMTestProxy proxy) {
            myProxy = proxy;
        }

        @Override
        public void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            if (myProxy.isSuite()) {
                // for suite returns brief statistics
                TestsPresentationUtil.appendSuiteStatusColorPresentation(myProxy, this);
            }
            else {
                // for test returns test status string
                TestsPresentationUtil.appendTestStatusColorPresentation(myProxy, this);
            }
        }
    }
}
