package consulo.execution.coverage;

import consulo.project.Project;
import consulo.util.xml.serializer.JDOMExternalizable;
import com.intellij.rt.coverage.data.ProjectData;
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

    @Nullable ProjectData getCoverageData(CoverageDataManager coverageDataManager);

    void setCoverageData(ProjectData projectData);

    void restoreCoverageData();

    boolean isTrackTestFolders();

    boolean isTracingEnabled();

    CoverageRunner getRunner();

    
    CoverageEngine getCoverageEngine();

    Project getProject();
}
