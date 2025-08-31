package consulo.task.event;

import consulo.task.LocalTask;

/**
 * @author evgeny.zakrevsky
 * @since 2012-11-08
 */
public class TaskListenerAdapter implements TaskListener {
  @Override
  public void taskDeactivated(LocalTask task) {
  }

  @Override
  public void taskActivated(LocalTask task) {
  }

  @Override
  public void taskAdded(LocalTask task) {
  }

  @Override
  public void taskRemoved(LocalTask task) {
  }
}
