package consulo.execution.configuration;

import consulo.project.Project;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author traff
 */
public abstract class AbstractRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {
  private final Map<String, String> myEnvs = new LinkedHashMap<>();
  private boolean myPassParentEnvs = true;

  public AbstractRunConfiguration(String name, RunConfigurationModule configurationModule, ConfigurationFactory factory) {
    super(name, configurationModule, factory);
  }

  public AbstractRunConfiguration(Project project, ConfigurationFactory factory) {
    super(new RunConfigurationModule(project), factory);
  }

  public Map<String, String> getEnvs() {
    return myEnvs;
  }

  public void setEnvs(final Map<String, String> envs) {
    myEnvs.clear();
    myEnvs.putAll(envs);
  }

  public boolean isPassParentEnvs() {
    return myPassParentEnvs;
  }

  public void setPassParentEnvs(final boolean passParentEnvs) {
    myPassParentEnvs = passParentEnvs;
  }
}
