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
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.sm.ui.TestsPresentationUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;

/**
 * @author Roman Chernyatchik
 */
public class ColumnDuration extends BaseColumn implements Comparator<SMTestProxy> {
    public ColumnDuration() {
        super(SMTestLocalize.smTestRunnerUiTabsStatisticsColumnsDurationTitle());
    }

    @Override
    public String valueOf(SMTestProxy testProxy) {
        return TestsPresentationUtil.getDurationPresentation(testProxy);
    }

    @Override
    @Nullable
    public Comparator<SMTestProxy> getComparator() {
        return this;
    }

    @Override
    public int compare(SMTestProxy proxy1, SMTestProxy proxy2) {
        Long duration1 = proxy1.getDuration();
        Long duration2 = proxy2.getDuration();

        if (duration1 == null) {
            return duration2 == null ? 0 : -1;
        }
        if (duration2 == null) {
            return +1;
        }
        return duration1.compareTo(duration2);
    }


    @Override
    public TableCellRenderer getRenderer(SMTestProxy proxy) {
        return new DurationCellRenderer(proxy);
    }

    public static class DurationCellRenderer extends ColoredTableCellRenderer implements ColoredRenderer {
        private final SMTestProxy myProxy;

        public DurationCellRenderer(SMTestProxy proxy) {
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

            SimpleTextAttributes attributes;
            if (myProxy.isSuite() && ColumnTest.TestsCellRenderer.isFirstLine(row)) {
                //Black bold for parent suite of items in statistics
                attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
            }
            else {
                //Black, regular for other suites and tests
                attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            }
            append(title, attributes);
        }
    }
}
