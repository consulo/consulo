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

import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.sm.runner.SMTestsRunnerBundle;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;

/**
 * @author Roman Chernyatchik
 */
public class ColumnTest extends BaseColumn implements Comparator<SMTestProxy> {
    public ColumnTest() {
        super(SMTestsRunnerBundle.message("sm.test.runner.ui.tabs.statistics.columns.test.title"));
    }

    @Nonnull
    @Override
    public String valueOf(SMTestProxy testProxy) {
        return testProxy.getPresentableName();
    }

    @Nullable
    @Override
    public Comparator<SMTestProxy> getComparator() {
        return this;
    }

    @Override
    public int compare(SMTestProxy proxy1, SMTestProxy proxy2) {
        return proxy1.getName().compareTo(proxy2.getName());
    }

    @Override
    public TableCellRenderer getRenderer(SMTestProxy proxy) {
        return new TestsCellRenderer(proxy);
    }

    public static class TestsCellRenderer extends ColoredTableCellRenderer implements ColoredRenderer {
        @NonNls
        private static final String TOTAL_TITLE = SMTestsRunnerBundle.message("sm.test.runner.ui.tabs.statistics.columns.test.total.title");
        @NonNls
        private static final String PARENT_TITLE = "..";

        private final SMTestProxy myProxy;

        public TestsCellRenderer(SMTestProxy proxy) {
            myProxy = proxy;
        }

        @Override
        public void customizeCellRenderer(
            JTable table,
            Object value,
            boolean selected,
            boolean hasFocus,
            int row,
            int column
        ) {
            assert value != null;

            String title = value.toString();
            //Black bold for with caption "Total" for parent suite of items in statistics
            if (myProxy.isSuite() && isFirstLine(row)) {
                if (myProxy.getParent() == null) {
                    append(TOTAL_TITLE, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
                else {
                    append(PARENT_TITLE, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    append(" (" + myProxy.getName() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                return;
            }
            //Black, regular for other suites and tests
            append(title, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        public static boolean isFirstLine(int row) {
            return row == 0;
        }
    }
}
