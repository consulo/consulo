package consulo.ide.impl.idea.openapi.externalSystem;

import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.openapi.externalSystem.task.ExternalSystemTaskManager;
import consulo.ide.impl.idea.openapi.externalSystem.model.ProjectSystemId;
import consulo.ide.impl.idea.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import consulo.ide.impl.idea.openapi.externalSystem.service.ParametersEnhancer;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.ide.impl.idea.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import consulo.ide.impl.idea.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import consulo.ide.impl.idea.openapi.externalSystem.settings.ExternalProjectSettings;
import consulo.ide.impl.idea.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Pair;
import consulo.ide.impl.idea.util.Function;
import javax.annotation.Nonnull;

/**
 * IntelliJ external systems integration is built using GoF Bridge pattern, i.e. 'external-system' module defines
 * external system-specific extension (current interface) and an api which is used by all extensions. Most of the codebase
 * is built on top of that api and provides generic actions like 'sync ide project with external project'; 'import library
 * dependencies which are configured at external system but not at the ide' etc.
 * <p/>
 * That makes it relatively easy to add a new external system integration.
 * 
 * @author Denis Zhdanov
 * @since 4/4/13 4:05 PM
 */
public interface ExternalSystemManager<
  ProjectSettings extends ExternalProjectSettings,
  SettingsListener extends ExternalSystemSettingsListener<ProjectSettings>,
  Settings extends AbstractExternalSystemSettings<Settings, ProjectSettings, SettingsListener>,
  LocalSettings extends AbstractExternalSystemLocalSettings,
  ExecutionSettings extends ExternalSystemExecutionSettings>
  extends ParametersEnhancer
{
  
  ExtensionPointName<ExternalSystemManager> EP_NAME = ExtensionPointName.create("consulo.externalSystemManager");
  
  /**
   * @return    id of the external system represented by the current manager
   */
  @Nonnull
  ProjectSystemId getSystemId();

  /**
   * @return    a strategy which can be queried for external system settings to use with the given project
   */
  @Nonnull
  Function<Project, Settings> getSettingsProvider();

  /**
   * @return    a strategy which can be queried for external system local settings to use with the given project
   */
  @Nonnull
  Function<Project, LocalSettings> getLocalSettingsProvider();

  /**
   * @return    a strategy which can be queried for external system execution settings to use with the given project
   */
  @Nonnull
  Function<Pair<Project, String/*linked project path*/>, ExecutionSettings> getExecutionSettingsProvider();

  /**
   * Allows to retrieve information about {@link ExternalSystemProjectResolver project resolver} to use for the target external
   * system.
   * <p/>
   * <b>Note:</b> we return a class instance instead of resolver object here because there is a possible case that the resolver
   * is used at external (non-ide) process, so, it needs information which is enough for instantiating it there. That implies
   * the requirement that target resolver class is expected to have a no-args constructor
   * 
   * @return  class of the project resolver to use for the target external system
   */
  @Nonnull
  Class<? extends ExternalSystemProjectResolver<ExecutionSettings>> getProjectResolverClass();

  /**
   * @return    class of the build manager to use for the target external system
   * @see #getProjectResolverClass()
   */
  Class<? extends ExternalSystemTaskManager<ExecutionSettings>> getTaskManagerClass();

  /**
   * @return    file chooser descriptor to use when adding new external project
   */
  @Nonnull
  FileChooserDescriptor getExternalProjectDescriptor();
}
