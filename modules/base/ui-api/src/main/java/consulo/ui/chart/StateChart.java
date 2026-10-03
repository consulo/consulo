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
package consulo.ui.chart;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.internal.UIInternal;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public interface StateChart<S> extends Component {
    static <S> StateChart<S> create(TimeAxis axis) {
        return UIInternal.get()._Components_stateChart(axis);
    }

    TimeAxis getAxis();

    @RequiredUIAccess
    void setStatePresentation(S state, LocalizeValue label, ColorValue color);

    @RequiredUIAccess
    StateRow<S> addRow(LocalizeValue name);

    @RequiredUIAccess
    void removeRow(StateRow<S> row);

    List<StateRow<S>> getRows();
}
