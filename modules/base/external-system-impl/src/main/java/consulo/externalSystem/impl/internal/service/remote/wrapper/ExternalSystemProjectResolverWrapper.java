package consulo.externalSystem.impl.internal.service.remote.wrapper;

import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.project.ProjectData;
import org.jspecify.annotations.Nullable;


/**
 * Intercepts calls to the target {@link RemoteExternalSystemProjectResolver} and
 * {@link ExternalSystemTaskNotificationListener#onQueued(ExternalSystemTaskId) updates 'queued' task status}.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 2/8/12 7:21 PM
 */
public class ExternalSystemProjectResolverWrapper<S extends ExternalSystemExecutionSettings>
  extends AbstractRemoteExternalSystemServiceWrapper<S, RemoteExternalSystemProjectResolver<S>>
  implements RemoteExternalSystemProjectResolver<S>
{

  
  private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemProjectResolverWrapper(RemoteExternalSystemProjectResolver<S> delegate,
                                              RemoteExternalSystemProgressNotificationManager progressManager)
  {
    super(delegate);
    myProgressManager = progressManager;
  }

  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                                    String projectPath,
                                                    boolean isPreviewMode,
                                                    @Nullable S settings)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException
  {
    myProgressManager.onQueued(id);
    try {
      DataNode<ProjectData> projectDataNode = getDelegate().resolveProjectInfo(id, projectPath, isPreviewMode, settings);
      myProgressManager.onSuccess(id);
      return projectDataNode;
    }
    catch (ExternalSystemException e) {
      myProgressManager.onFailure(id, e);
      throw e;
    }
    catch (Exception e) {
      myProgressManager.onFailure(id, e);
      throw new ExternalSystemException(e);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }

  @Override
  public boolean cancelTask(ExternalSystemTaskId id)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    myProgressManager.onQueued(id);
    try {
      return getDelegate().cancelTask(id);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }
}
