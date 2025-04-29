package consulo.execution.coverage;

import com.intellij.rt.coverage.data.ProjectData;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author anna
 * @since 2008-02-13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CoverageRunner {
    public abstract ProjectData loadCoverageData(@Nonnull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite);

    public abstract String getPresentableName();

    public abstract String getId();

    public abstract String getDataFileExtension();

    public abstract boolean acceptsCoverageEngine(@Nonnull CoverageEngine engine);

    public static <T extends CoverageRunner> T getInstance(@Nonnull Class<T> coverageRunnerClass) {
        return Application.get().getExtensionPoint(CoverageRunner.class).findExtensionOrFail(coverageRunnerClass);
    }

    public boolean isCoverageByTestApplicable() {
        return false;
    }
}