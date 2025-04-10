package consulo.externalSystem.impl.internal.service.remote.wrapper;

import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProgressNotificationManager;
import consulo.externalSystem.impl.internal.service.remote.RemoteExternalSystemProjectResolver;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.project.ProjectData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.rmi.RemoteException;

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

  @Nonnull
  private final RemoteExternalSystemProgressNotificationManager myProgressManager;

  public ExternalSystemProjectResolverWrapper(@Nonnull RemoteExternalSystemProjectResolver<S> delegate,
                                              @Nonnull RemoteExternalSystemProgressNotificationManager progressManager)
  {
    super(delegate);
    myProgressManager = progressManager;
  }

  @jakarta.annotation.Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@Nonnull ExternalSystemTaskId id,
                                                    @Nonnull String projectPath,
                                                    boolean isPreviewMode,
                                                    @Nullable S settings)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException, RemoteException
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
  public boolean cancelTask(@Nonnull ExternalSystemTaskId id)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException, RemoteException {
    myProgressManager.onQueued(id);
    try {
      return getDelegate().cancelTask(id);
    }
    finally {
      myProgressManager.onEnd(id);
    }
  }
}
