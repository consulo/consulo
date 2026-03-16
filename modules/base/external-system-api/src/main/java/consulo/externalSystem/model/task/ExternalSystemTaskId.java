package consulo.externalSystem.model.task;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents id of the task enqueued to external API for execution.
 *
 * @author Denis Zhdanov
 * @since 11/10/11 9:09 AM
 */
public class ExternalSystemTaskId implements Serializable {

  
  private static final AtomicLong COUNTER          = new AtomicLong();
  private static final          long       serialVersionUID = 1L;

  
  private final ExternalSystemTaskType myType;
  
  private final String                 myProjectId;
  
  private final ProjectSystemId        myProjectSystemId;

  private final long myId;

  private ExternalSystemTaskId(ProjectSystemId projectSystemId, ExternalSystemTaskType type, String projectId, long taskId) {
    myType = type;
    myProjectId = projectId;
    myProjectSystemId = projectSystemId;
    myId = taskId;
  }

  
  public String getIdeProjectId() {
    return myProjectId;
  }

  
  public ProjectSystemId getProjectSystemId() {
    return myProjectSystemId;
  }

  /**
   * Allows to retrieve distinct task id object of the given type.
   *
   * @param type     target task type
   * @param project  target ide project
   * @return         distinct task id object of the given type
   */
  
  public static ExternalSystemTaskId create(ProjectSystemId projectSystemId, ExternalSystemTaskType type, Project project) {
    return create(projectSystemId, type, getProjectId(project));
  }

  
  public static ExternalSystemTaskId create(ProjectSystemId projectSystemId, ExternalSystemTaskType type, String ideProjectId) {
    return new ExternalSystemTaskId(projectSystemId, type, ideProjectId, COUNTER.getAndIncrement());
  }

  
  public static String getProjectId(Project project) {
    return project.isDisposed() ? project.getName() : project.getName() + ":" + project.getLocationHash();
  }

  @Nullable
  public Project findProject() {
    ProjectManager projectManager = ProjectManager.getInstance();
    for (Project project : projectManager.getOpenProjects()) {
      if (myProjectId.equals(getProjectId(project))) return project;
    }
    return null;
  }

  
  public ExternalSystemTaskType getType() {
    return myType;
  }

  @Override
  public int hashCode() {
    return 31 * myType.hashCode() + (int)(myId ^ (myId >>> 32));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalSystemTaskId that = (ExternalSystemTaskId)o;
    return myId == that.myId && myType == that.myType;
  }

  @Override
  public String toString() {
    return myType + ":" + myId;
  }
}
