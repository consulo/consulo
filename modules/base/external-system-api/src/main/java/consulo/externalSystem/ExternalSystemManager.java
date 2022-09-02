package consulo.externalSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.service.ParametersEnhancer;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import java.util.function.Function;

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
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemManager<ProjectSettings extends ExternalProjectSettings, SettingsListener extends ExternalSystemSettingsListener<ProjectSettings>, Settings extends AbstractExternalSystemSettings<Settings, ProjectSettings, SettingsListener>, LocalSettings extends AbstractExternalSystemLocalSettings, ExecutionSettings extends ExternalSystemExecutionSettings>
        extends ParametersEnhancer {

  ExtensionPointName<ExternalSystemManager> EP_NAME = ExtensionPointName.create(ExternalSystemManager.class);

  /**
   * @return id of the external system represented by the current manager
   */
  @Nonnull
  ProjectSystemId getSystemId();

  /**
   * @return a strategy which can be queried for external system settings to use with the given project
   */
  @Nonnull
  Function<Project, Settings> getSettingsProvider();

  /**
   * @return a strategy which can be queried for external system local settings to use with the given project
   */
  @Nonnull
  Function<Project, LocalSettings> getLocalSettingsProvider();

  /**
   * @return a strategy which can be queried for external system execution settings to use with the given project
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
   * @return class of the project resolver to use for the target external system
   */
  @Nonnull
  Class<? extends ExternalSystemProjectResolver<ExecutionSettings>> getProjectResolverClass();

  /**
   * @return class of the build manager to use for the target external system
   * @see #getProjectResolverClass()
   */
  Class<? extends ExternalSystemTaskManager<ExecutionSettings>> getTaskManagerClass();

  /**
   * @return file chooser descriptor to use when adding new external project
   */
  @Nonnull
  FileChooserDescriptor getExternalProjectDescriptor();
}
