/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.chart;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopAWTChartPanel extends JPanel {
    private final IntSupplier myMinimumHeight;
    private final IntUnaryOperator myPreferredHeight;

    DesktopAWTChartPanel(LayoutManager layout, IntSupplier minimumHeight, IntUnaryOperator preferredHeight) {
        super(layout);
        myMinimumHeight = minimumHeight;
        myPreferredHeight = preferredHeight;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        if (isMinimumSizeSet()) {
            return size;
        }
        return new Dimension(size.width, myMinimumHeight.getAsInt());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (isPreferredSizeSet()) {
            return size;
        }
        return new Dimension(size.width, Math.max(myMinimumHeight.getAsInt(), myPreferredHeight.applyAsInt(size.height)));
    }
}
