package consulo.execution.coverage;

import consulo.project.Project;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.execution.coverage.data.CoverageProjectData;
import org.jspecify.annotations.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public interface CoverageSuite extends JDOMExternalizable {
    boolean isValid();

    
    String getCoverageDataFileName();

    String getPresentableName();

    long getLastCoverageTimeStamp();

    
    CoverageFileProvider getCoverageDataFileProvider();

    boolean isCoverageByTestApplicable();

    boolean isCoverageByTestEnabled();

    @Nullable CoverageProjectData getCoverageData(CoverageDataManager coverageDataManager);

    void setCoverageData(CoverageProjectData projectData);

    void restoreCoverageData();

    boolean isTrackTestFolders();

    boolean isTracingEnabled();

    CoverageRunner getRunner();

    
    CoverageEngine getCoverageEngine();

    Project getProject();
}
