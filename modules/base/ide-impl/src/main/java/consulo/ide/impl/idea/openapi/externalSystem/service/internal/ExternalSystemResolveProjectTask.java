package consulo.ide.impl.idea.openapi.externalSystem.service.internal;

import consulo.ide.ServiceManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.ide.impl.idea.openapi.externalSystem.service.ExternalSystemFacadeManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 1/24/12 7:21 AM
 */
public class ExternalSystemResolveProjectTask extends AbstractExternalSystemTask {

  private final AtomicReference<DataNode<ProjectData>> myExternalProject = new AtomicReference<DataNode<ProjectData>>();

  @Nonnull
  private final String myProjectPath;
  private final boolean myIsPreviewMode;

  public ExternalSystemResolveProjectTask(@Nonnull ProjectSystemId externalSystemId,
                                          @Nonnull Project project,
                                          @Nonnull String projectPath,
                                          boolean isPreviewMode) {
    super(externalSystemId, ExternalSystemTaskType.RESOLVE_PROJECT, project, projectPath);
    myProjectPath = projectPath;
    myIsPreviewMode = isPreviewMode;
  }

  @SuppressWarnings("unchecked")
  protected void doExecute() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    Project ideProject = getIdeProject();
    RemoteExternalSystemProjectResolver resolver = manager.getFacade(ideProject, myProjectPath, getExternalSystemId()).getResolver();
    ExternalSystemExecutionSettings settings = ExternalSystemApiUtil.getExecutionSettings(ideProject, myProjectPath, getExternalSystemId());

    DataNode<ProjectData> project = resolver.resolveProjectInfo(getId(), myProjectPath, myIsPreviewMode, settings);

    if (project == null) {
      return;
    }
    myExternalProject.set(project);
  }

  protected boolean doCancel() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    Project ideProject = getIdeProject();
    RemoteExternalSystemProjectResolver resolver = manager.getFacade(ideProject, myProjectPath, getExternalSystemId()).getResolver();

    return resolver.cancelTask(getId());
  }

  @jakarta.annotation.Nullable
  public DataNode<ProjectData> getExternalProject() {
    return myExternalProject.get();
  }

  @Override
  @Nonnull
  protected String wrapProgressText(@Nonnull String text) {
    return ExternalSystemBundle.message("progress.update.text", getExternalSystemId().getReadableName(), text);
  }
}
