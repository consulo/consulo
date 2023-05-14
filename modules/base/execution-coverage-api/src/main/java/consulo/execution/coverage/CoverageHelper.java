package consulo.execution.coverage;

import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunnerSettings;
import consulo.process.ProcessHandler;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author traff
 */
public class CoverageHelper {
  private CoverageHelper() {
  }

  public static void attachToProcess(@Nonnull RunConfigurationBase configuration,
                                     @Nonnull ProcessHandler handler,
                                     RunnerSettings runnerSettings) {
    resetCoverageSuit(configuration);

    // attach to process termination listener
    CoverageDataManager.getInstance(configuration.getProject()).attachToProcess(handler, configuration, runnerSettings);
  }

  public static void resetCoverageSuit(RunConfigurationBase configuration) {
    final CoverageEnabledConfiguration covConfig = CoverageEnabledConfiguration.getOrCreate(configuration);

    // reset coverage suite
    covConfig.setCurrentCoverageSuite(null);

    // register new coverage suite
    final CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(configuration.getProject());

    covConfig.setCurrentCoverageSuite(coverageDataManager.addCoverageSuite(covConfig));
  }

  public static void doReadExternal(RunConfigurationBase runConfiguration, Element element) throws InvalidDataException {
    final CoverageEnabledConfiguration covConf = CoverageEnabledConfiguration.getOrCreate(runConfiguration);

    covConf.readExternal(element);
  }

  public static void doWriteExternal(RunConfigurationBase runConfiguration, Element element) throws WriteExternalException {
    final CoverageEnabledConfiguration covConf = CoverageEnabledConfiguration.getOrCreate(runConfiguration);

    covConf.writeExternal(element);
  }
}
