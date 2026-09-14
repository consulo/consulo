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
package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CoverageProjectDataImpl implements CoverageProjectData {
    private final Map<String, CoverageUnit> myUnits = new HashMap<>();

    @Nullable
    @Override
    public CoverageUnit getUnit(String name) {
        return myUnits.get(name);
    }

    @Override
    public CoverageUnit getOrCreateUnit(String name) {
        return myUnits.computeIfAbsent(name, CoverageUnitImpl::new);
    }

    @Override
    public Collection<CoverageUnit> getUnits() {
        return myUnits.values();
    }

    @Override
    public void merge(CoverageProjectData data) {
        for (CoverageUnit unit : data.getUnits()) {
            CoverageUnit mine = myUnits.get(unit.getName());
            if (mine == null) {
                mine = getOrCreateUnit(unit.getName());
            }
            if (mine instanceof CoverageUnitImpl unitImpl) {
                unitImpl.merge(unit);
            }
        }
    }
}
