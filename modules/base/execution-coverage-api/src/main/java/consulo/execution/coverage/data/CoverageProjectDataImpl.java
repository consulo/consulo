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
            myUnits.putIfAbsent(unit.getName(), unit);
        }
    }
}
