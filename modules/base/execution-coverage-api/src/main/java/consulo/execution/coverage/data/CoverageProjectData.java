package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface CoverageProjectData {
    @Nullable
    CoverageUnit getUnit(String name);

    CoverageUnit getOrCreateUnit(String name);

    Collection<CoverageUnit> getUnits();

    void merge(CoverageProjectData data);
}
