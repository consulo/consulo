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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.StateChart;
import consulo.ui.chart.StateRow;
import consulo.ui.chart.TimeAxis;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.chart.StatePresentation;
import consulo.ui.impl.chart.StateRowImpl;
import consulo.ui.impl.chart.TimeAxisImpl;
import consulo.ui.impl.chart.TimeAxisSubscription;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessStateChart<S> extends HeadlessComponentBase implements StateChart<S> {
    private final TimeAxisImpl myAxis;
    private final TimeAxisSubscription mySubscription;
    private final Map<S, StatePresentation> myPresentations = new LinkedHashMap<>();
    private final List<StateRowImpl<S>> myRows = new ArrayList<>();

    public HeadlessStateChart(TimeAxis axis) {
        myAxis = (TimeAxisImpl) axis;
        mySubscription = new TimeAxisSubscription(myAxis, List.of());
        mySubscription.activate();
    }

    public void setAttached(boolean attached) {
        if (attached) {
            mySubscription.activate();
        }
        else {
            mySubscription.deactivate();
        }
    }

    public @Nullable StatePresentation getPresentation(S state) {
        return myPresentations.get(state);
    }

    @Override
    public TimeAxis getAxis() {
        return myAxis;
    }

    @RequiredUIAccess
    @Override
    public void setStatePresentation(S state, LocalizeValue label, ColorValue color) {
        myPresentations.put(state, new StatePresentation(label, color));
    }

    @RequiredUIAccess
    @Override
    public StateRow<S> addRow(LocalizeValue name) {
        StateRowImpl<S> row = new StateRowImpl<>(name, () -> {
        });
        myRows.add(row);
        return row;
    }

    @RequiredUIAccess
    @Override
    public void removeRow(StateRow<S> row) {
        myRows.remove(row);
    }

    @Override
    public List<StateRow<S>> getRows() {
        return List.copyOf(myRows);
    }
}
