package consulo.execution.coverage;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.container.boot.ContainerPathManager;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * Base class for run configurations with enabled code coverage
 *
 * @author ven
 */
public abstract class CoverageEnabledConfiguration {
    private static final Logger LOG = Logger.getInstance(CoverageEnabledConfiguration.class);

    public static final Key<CoverageEnabledConfiguration> COVERAGE_KEY = Key.create("consulo.ide.impl.idea.coverage");

    private final Project myProject;
    private final RunConfigurationBase myConfiguration;

    private boolean myIsCoverageEnabled;
    private @Nullable String myRunnerId;
    private boolean myTrackPerTestCoverage;
    private boolean mySampling = true;
    private boolean myTrackTestFolders;

    protected String myCoverageFilePath;
    private CoverageSuite myCurrentCoverageSuite;

    public CoverageEnabledConfiguration(RunConfigurationBase configuration) {
        myConfiguration = configuration;
        myProject = configuration.getProject();
    }

    public RunConfigurationBase getConfiguration() {
        return myConfiguration;
    }

    public @Nullable String getRunnerId() {
        return myRunnerId;
    }

    public @Nullable CoverageRunner getCoverageRunner() {
        String runnerId = myRunnerId;
        if (runnerId == null) {
            return null;
        }
        return Application.get().getExtensionPoint(CoverageRunner.class).findFirstSafe(runner -> runnerId.equals(runner.getId()));
    }

    /**
     * The coverage engine sets the runner from its project coverage settings when only one runner fits.
     */
    public void setCoverageRunner(@Nullable CoverageRunner coverageRunner) {
        myRunnerId = coverageRunner != null ? coverageRunner.getId() : null;
        myCoverageFilePath = null;
    }

    public boolean isTrackTestFolders() {
        return myTrackTestFolders;
    }

    public void setTrackTestFolders(boolean trackTestFolders) {
        myTrackTestFolders = trackTestFolders;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public boolean isCoverageEnabled() {
        return myIsCoverageEnabled;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public void setCoverageEnabled(boolean isCoverageEnabled) {
        myIsCoverageEnabled = isCoverageEnabled;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public boolean isSampling() {
        return mySampling;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public void setSampling(boolean sampling) {
        mySampling = sampling;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public boolean isTrackPerTestCoverage() {
        return myTrackPerTestCoverage;
    }

    @Deprecated
    @DeprecationInfo("Moved to coverage settings")
    public void setTrackPerTestCoverage(boolean trackPerTestCoverage) {
        myTrackPerTestCoverage = trackPerTestCoverage;
    }

    public CoverageSuite getCurrentCoverageSuite() {
        return myCurrentCoverageSuite;
    }

    public void setCurrentCoverageSuite(CoverageSuite currentCoverageSuite) {
        myCurrentCoverageSuite = currentCoverageSuite;
    }

    public String getName() {
        return myConfiguration.getName();
    }

    public boolean canHavePerTestCoverage() {
        return Application.get().getExtensionPoint(CoverageEngine.class)
            .anyMatchSafe(engine -> engine.isApplicableTo(myConfiguration) && engine.canHavePerTestCoverage(myConfiguration));
    }

    public static boolean isApplicableTo(RunConfigurationBase runConfiguration) {
        CoverageEnabledConfiguration configuration = runConfiguration.getCopyableUserData(COVERAGE_KEY);
        //noinspection SimplifiableIfStatement
        if (configuration != null) {
            return true;
        }

        return Application.get().getExtensionPoint(CoverageEngine.class)
            .anyMatchSafe(engine -> engine.isApplicableTo(runConfiguration));
    }

    
    public static CoverageEnabledConfiguration getOrCreate(RunConfigurationBase runConfiguration) {
        CoverageEnabledConfiguration configuration = runConfiguration.getCopyableUserData(COVERAGE_KEY);
        if (configuration == null) {
            configuration = Application.get().getExtensionPoint(CoverageEngine.class).computeSafeIfAny(
                engine -> engine.isApplicableTo(runConfiguration)
                    ? engine.createCoverageEnabledConfiguration(runConfiguration)
                    : null
            );
            LOG.assertTrue(
                configuration != null,
                "Coverage enabled run configuration wasn't found for run configuration: " + runConfiguration.getName() +
                    ", type = " + runConfiguration.getClass().getName()
            );
            runConfiguration.putCopyableUserData(COVERAGE_KEY, configuration);
        }
        return configuration;
    }

    public @Nullable String getCoverageFilePath() {
        if (myCoverageFilePath == null) {
            myCoverageFilePath = createCoverageFile();
        }
        return myCoverageFilePath;
    }

    protected @Nullable String createCoverageFile() {
        if (getCoverageRunner() == null) {
            return null;
        }

        String coverageRootPath = ContainerPathManager.get().getSystemPath() + File.separator + "coverage";
        String path = coverageRootPath + File.separator + myProject.getName() + coverageFileNameSeparator()
            + FileUtil.sanitizeFileName(myConfiguration.getName()) + ".coverage";

        new File(coverageRootPath).mkdirs();
        return path;
    }

    protected String coverageFileNameSeparator() {
        return "$";
    }
}
