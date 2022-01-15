package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.util.Producer;
import javax.annotation.Nonnull;

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

  @javax.annotation.Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@Nonnull final ExternalSystemTaskId id,
                                                  @Nonnull final String projectPath,
                                                  final boolean isPreviewMode,
                                                  ExternalSystemExecutionSettings settings)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException
  {
    return execute(id, new Producer<DataNode<ProjectData>>() {
      @javax.annotation.Nullable
      @Override
      public DataNode<ProjectData> produce() {
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
