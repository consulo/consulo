package consulo.sandboxPlugin.ide.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageProjectDataImpl;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author VISTALL
 * @since 2026-10-23
 */
@ExtensionImpl
public class SandCoverageRunner extends CoverageRunner {
    @Override
    public CoverageProjectData loadCoverageData(File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
        return new CoverageProjectDataImpl();
    }

    @Override
    public String getPresentableName() {
        return "Sand Coverage";
    }

    @Override
    public String getId() {
        return "sand";
    }

    @Override
    public String getDataFileExtension() {
        return "sandcov";
    }

    @Override
    public boolean acceptsCoverageEngine(CoverageEngine engine) {
        return engine instanceof SandCoverageEngine;
    }
}
