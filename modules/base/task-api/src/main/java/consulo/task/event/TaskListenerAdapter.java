package consulo.task.event;

import consulo.task.LocalTask;

/**
 * @author evgeny.zakrevsky
 * @since 2012-11-08
 */
public class TaskListenerAdapter implements TaskListener {
  @Override
  public void taskDeactivated(final LocalTask task) {
  }

  @Override
  public void taskActivated(final LocalTask task) {
  }

  @Override
  public void taskAdded(final LocalTask task) {
  }

  @Override
  public void taskRemoved(final LocalTask task) {
  }
}
