package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtilRt;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Basic run configuration type for external system tasks.
 *
 * @author Denis Zhdanov
 * @since 23.05.13 17:43
 */
public abstract class AbstractExternalSystemTaskConfigurationType implements ConfigurationType {

  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final ConfigurationFactory[] myFactories = new ConfigurationFactory[1];

  @Nonnull
  private final NotNullLazyValue<Image> myIcon = new NotNullLazyValue<Image>() {
    @Nonnull
    @Override
    protected Image compute() {
      ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
      Image result = null;
      if (manager instanceof ExternalSystemUiAware) {
        result = ((ExternalSystemUiAware)manager).getProjectIcon();
      }
      return result == null ? DefaultExternalSystemUiAware.INSTANCE.getTaskIcon() : result;
    }
  };

  protected AbstractExternalSystemTaskConfigurationType(@Nonnull final ProjectSystemId externalSystemId) {
    myExternalSystemId = externalSystemId;
    myFactories[0] = new ConfigurationFactory(this) {
      @Override
      public RunConfiguration createTemplateConfiguration(Project project) {
        return doCreateConfiguration(myExternalSystemId, project, this, "");
      }
    };
  }

  @Nonnull
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  @Nonnull
  public ConfigurationFactory getFactory() {
    return myFactories[0];
  }

  @SuppressWarnings("MethodMayBeStatic")
  @Nonnull
  protected ExternalSystemRunConfiguration doCreateConfiguration(@Nonnull ProjectSystemId externalSystemId,
                                                                 @Nonnull Project project,
                                                                 @Nonnull ConfigurationFactory factory,
                                                                 @Nonnull String name)
  {
    return new ExternalSystemRunConfiguration(externalSystemId, project, factory, name);
  }

  @Override
  public String getDisplayName() {
    return myExternalSystemId.getReadableName();
  }

  @Override
  public String getConfigurationTypeDescription() {
    return ExternalSystemBundle.message("run.configuration.description", myExternalSystemId.getReadableName());
  }

  @Override
  public Image getIcon() {
    return myIcon.getValue();
  }

  @Nonnull
  @Override
  public String getId() {
    return myExternalSystemId.getReadableName() + "RunConfiguration";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return myFactories;
  }

  @Nonnull
  public static String generateName(@Nonnull Project project, @Nonnull ExternalSystemTaskExecutionSettings settings) {
    return generateName(project, settings.getExternalSystemId(), settings.getExternalProjectPath(), settings.getTaskNames());
  }

  @Nonnull
  public static String generateName(@Nonnull Project project, @Nonnull ExternalTaskPojo task, @Nonnull ProjectSystemId externalSystemId) {
    return generateName(project, externalSystemId, task.getLinkedExternalProjectPath(), Collections.singletonList(task.getName()));
  }

  @Nonnull
  public static String generateName(@Nonnull Project project,
                                    @Nonnull ProjectSystemId externalSystemId,
                                    @javax.annotation.Nullable String externalProjectPath,
                                    @Nonnull List<String> taskNames)
  {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    AbstractExternalSystemSettings<?, ?,?> s = manager.getSettingsProvider().fun(project);
    Map<String/* project dir path */, String/* project file path */> rootProjectPaths = ContainerUtilRt.newHashMap();
    for (ExternalProjectSettings projectSettings : s.getLinkedProjectsSettings()) {
      String path = projectSettings.getExternalProjectPath();
      if(path == null) continue;
      final File rootProjectPathFile = new File(path).getParentFile();
      if(rootProjectPathFile == null) continue;
      rootProjectPaths.put(rootProjectPathFile.getAbsolutePath(), path);
    }
    
    String rootProjectPath = null;
    if (externalProjectPath != null) {
      if (!rootProjectPaths.containsKey(externalProjectPath)) {
        for (File f = new File(externalProjectPath), prev = null;
             f != null && !FileUtil.filesEqual(f, prev);
             prev = f, f = f.getParentFile())
        {
          rootProjectPath = rootProjectPaths.get(f.getAbsolutePath());
          if (rootProjectPath != null) {
            break;
          }
        }
      }
    }
    
    StringBuilder buffer = new StringBuilder();
    
    final String projectName;
    if (rootProjectPath == null) {
      projectName = null;
    }
    else {
      projectName = ExternalSystemApiUtil.getProjectRepresentationName(externalProjectPath, rootProjectPath);
    }
    if (!StringUtil.isEmptyOrSpaces(projectName)) {
      buffer.append(projectName);
      buffer.append(" ");
    }

    buffer.append("[");
    if (!taskNames.isEmpty()) {
      for (String taskName : taskNames) {
        buffer.append(taskName).append(" ");
      }
      buffer.setLength(buffer.length() - 1);
    }
    buffer.append("]");

    return buffer.toString();
  }
}
