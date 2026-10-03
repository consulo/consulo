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
package consulo.ui.impl.chart;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.StateRow;

import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class StateRowImpl<S> implements StateRow<S> {
    private final LocalizeValue myName;
    private final StateRowData<S> myData = new StateRowData<>();
    private final Runnable myChanged;

    public StateRowImpl(LocalizeValue name, Runnable changed) {
        myName = name;
        myChanged = changed;
    }

    public StateRowData<S> getData() {
        return myData;
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @RequiredUIAccess
    @Override
    public void set(Instant time, S state) {
        if (myData.set(ChartTime.toMicros(time), state)) {
            myChanged.run();
        }
    }
}
