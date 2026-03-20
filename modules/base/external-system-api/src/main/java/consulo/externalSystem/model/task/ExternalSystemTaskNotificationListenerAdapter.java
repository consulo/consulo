package consulo.externalSystem.model.task;

/**
 * @author Denis Zhdanov
 * @since 11/10/11 12:18 PM
 */
public abstract class ExternalSystemTaskNotificationListenerAdapter implements ExternalSystemTaskNotificationListener {

  
  public static final ExternalSystemTaskNotificationListener NULL_OBJECT = new ExternalSystemTaskNotificationListenerAdapter() { };

  @Override
  public void onQueued(ExternalSystemTaskId id) {
  }

  @Override
  public void onStart(ExternalSystemTaskId id) {
  }

  @Override
  public void onStatusChange(ExternalSystemTaskNotificationEvent event) {
  }

  @Override
  public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
  }

  @Override
  public void onEnd(ExternalSystemTaskId id) {
  }

  @Override
  public void onSuccess(ExternalSystemTaskId id) {
  }

  @Override
  public void onFailure(ExternalSystemTaskId id, Exception e) {
  }
}
