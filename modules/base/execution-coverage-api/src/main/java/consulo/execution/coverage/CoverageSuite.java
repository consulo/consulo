package consulo.execution.coverage;

import consulo.project.Project;
import consulo.util.xml.serializer.JDOMExternalizable;
import com.intellij.rt.coverage.data.ProjectData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageSuite extends JDOMExternalizable {
    boolean isValid();

    @Nonnull
    String getCoverageDataFileName();

    String getPresentableName();

    long getLastCoverageTimeStamp();

    @Nonnull
    CoverageFileProvider getCoverageDataFileProvider();

    boolean isCoverageByTestApplicable();

    boolean isCoverageByTestEnabled();

    @Nullable
    ProjectData getCoverageData(CoverageDataManager coverageDataManager);

    void setCoverageData(ProjectData projectData);

    void restoreCoverageData();

    boolean isTrackTestFolders();

    boolean isTracingEnabled();

    CoverageRunner getRunner();

    @Nonnull
    CoverageEngine getCoverageEngine();

    Project getProject();
}
