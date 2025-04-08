/*
 * User: anna
 * Date: 13-Feb-2008
 */
package consulo.execution.coverage;

import com.intellij.rt.coverage.data.ProjectData;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CoverageRunner {
    public static final ExtensionPointName<CoverageRunner> EP_NAME = ExtensionPointName.create(CoverageRunner.class);

    public abstract ProjectData loadCoverageData(@Nonnull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite);

    public abstract String getPresentableName();

    public abstract String getId();

    public abstract String getDataFileExtension();

    public abstract boolean acceptsCoverageEngine(@Nonnull CoverageEngine engine);

    public static <T extends CoverageRunner> T getInstance(@Nonnull Class<T> coverageRunnerClass) {
        for (CoverageRunner coverageRunner : EP_NAME.getExtensionList()) {
            if (coverageRunnerClass.isInstance(coverageRunner)) {
                return coverageRunnerClass.cast(coverageRunner);
            }
        }
        assert false;
        return null;
    }

    public boolean isCoverageByTestApplicable() {
        return false;
    }
}