package consulo.externalSystem.impl.internal.service.remote;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.service.project.ProjectData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * Defines common interface for resolving gradle project, i.e. building object-level representation of <code>'build.gradle'</code>.
 * 
 * @author Denis Zhdanov
 * @since 8/8/11 10:58 AM
 */
public class RemoteExternalSystemProjectResolverImpl<S extends ExternalSystemExecutionSettings>
  extends AbstractRemoteExternalSystemService<S> implements RemoteExternalSystemProjectResolver<S>
{

  private final ExternalSystemProjectResolver<S> myDelegate;

  public RemoteExternalSystemProjectResolverImpl(@Nonnull ExternalSystemProjectResolver<S> delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@Nonnull final ExternalSystemTaskId id,
                                                  @Nonnull final String projectPath,
                                                  final boolean isPreviewMode,
                                                  ExternalSystemExecutionSettings settings)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException
  {
    return execute(id, new Supplier<DataNode<ProjectData>>() {
      @Nullable
      @Override
      public DataNode<ProjectData> get() {
        return myDelegate.resolveProjectInfo(id, projectPath, isPreviewMode, getSettings(), getNotificationListener());
      }
    });
  }

  @Override
  public boolean cancelTask(@Nonnull final ExternalSystemTaskId id)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    return myDelegate.cancelTask(id, getNotificationListener());
  }
}
