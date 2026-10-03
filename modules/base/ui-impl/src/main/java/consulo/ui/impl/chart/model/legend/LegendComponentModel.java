/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.ui.impl.chart.model.legend;

import consulo.ui.impl.chart.model.AspectModel;
import consulo.ui.impl.chart.model.Range;
import java.util.ArrayList;
import java.util.List;

public class LegendComponentModel extends AspectModel<LegendComponentModel.Aspect> {

    public enum Aspect {
        LEGEND,
    }

    private final List<Legend> myLegends;

    public LegendComponentModel() {
        myLegends = new ArrayList<>();
    }

    public LegendComponentModel(Range dependentRange) {
        this();
        // TODO(b/117123979) Move this dependency into Legend.
        dependentRange.addDependency(this).onChange(Range.Aspect.RANGE, () -> changed(Aspect.LEGEND));
    }

    public List<Legend> getLegends() {
        return myLegends;
    }

    public void add(Legend legend) {
        myLegends.add(legend);
        changed(Aspect.LEGEND);
    }

    public void remove(Legend legend) {
        myLegends.remove(legend);
        changed(Aspect.LEGEND);
    }
}
