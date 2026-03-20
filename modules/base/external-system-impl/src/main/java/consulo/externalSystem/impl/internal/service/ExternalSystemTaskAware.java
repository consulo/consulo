package consulo.externalSystem.impl.internal.service;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskType;

import java.util.Map;
import java.util.Set;

/**
 * Represents a service that exposes information about the tasks being processed.
 *
 * @author Denis Zhdanov
 * @since 2/8/12 1:46 PM
 */
public interface ExternalSystemTaskAware {

  boolean isTaskInProgress(ExternalSystemTaskId id);

  boolean cancelTask(ExternalSystemTaskId id);

  Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress();
}
