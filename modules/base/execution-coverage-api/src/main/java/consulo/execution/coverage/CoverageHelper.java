package consulo.execution.coverage;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunnerSettings;
import consulo.process.ProcessHandler;

/**
 * @author traff
 */
public class CoverageHelper {
    private CoverageHelper() {
    }

    public static void attachToProcess(
        RunConfigurationBase configuration,
        ProcessHandler handler,
        RunnerSettings runnerSettings
    ) {
        resetCoverageSuit(configuration);

        // attach to process termination listener
        CoverageDataManager.getInstance(configuration.getProject()).attachToProcess(handler, configuration, runnerSettings);
    }

    public static void resetCoverageSuit(RunConfigurationBase configuration) {
        CoverageEnabledConfiguration covConfig = CoverageEnabledConfiguration.getOrCreate(configuration);

        // reset coverage suite
        covConfig.setCurrentCoverageSuite(null);

        // register new coverage suite
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(configuration.getProject());

        covConfig.setCurrentCoverageSuite(coverageDataManager.addCoverageSuite(covConfig));
    }
}
