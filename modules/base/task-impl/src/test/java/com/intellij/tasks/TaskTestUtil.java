package com.intellij.tasks;

import static junit.framework.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.tasks.impl.TaskUtil;
import consulo.ui.image.Image;

/**
 * @author Mikhail Golubev
 */
public class TaskTestUtil {
  public static void  assertTasksEqual(@Nonnull Task t1, @Nonnull Task t2) {
    assertTrue(TaskUtil.tasksEqual(t1, t2));
  }

  public static void  assertTasksEqual(@Nonnull List<? extends Task> t1, @Nonnull List<? extends Task> t2) {
    assertTrue(TaskUtil.tasksEqual(t1, t2));
  }

  public static void  assertTasksEqual(@Nonnull Task[] t1, @Nonnull Task[] t2) {
    assertTrue(TaskUtil.tasksEqual(t1, t2));
  }

  /**
   * Auxiliary builder class to simplify comparison of server responses parsing results.
   *
   * @see #assertTasksEqual(Task, Task)
   */
  public static class TaskBuilder extends Task {
    private String myId;
    private String mySummary;
    private TaskRepository myRepository;
    private String myDescription;
    private String myIssueUrl;
    private Comment[] myComments = Comment.EMPTY_ARRAY;
    private Image myIcon;
    private TaskType myType = TaskType.OTHER;
    private TaskState myState;
    private Date myCreated;
    private Date myUpdated;
    private boolean myClosed = false;
    private boolean myIssue = true;

    public TaskBuilder(String id, String summary, TaskRepository repository) {
      myId = id;
      mySummary = summary;
      myRepository = repository;
    }

    public TaskBuilder withDescription(@Nullable String description) {
      myDescription = description;
      return this;
    }

    public TaskBuilder withIssueUrl(@Nullable String issueUrl) {
      myIssueUrl = issueUrl;
      return this;
    }

    public TaskBuilder withComments(@Nonnull Comment... comments) {
      myComments = comments;
      return this;
    }

    public TaskBuilder withClosed(boolean isClosed) {
      myClosed = isClosed;
      return this;
    }

    public TaskBuilder withIssue(boolean isIssue) {
      myIssue = isIssue;
      return this;
    }

    public TaskBuilder withUpdated(@Nullable Date updated) {
      myUpdated = updated;
      return this;
    }

    public TaskBuilder withUpdated(@Nonnull String updated) {
      return withUpdated(TaskUtil.parseDate(updated));
    }

    public TaskBuilder withCreated(@Nullable Date created) {
      myCreated = created;
      return this;
    }

    public TaskBuilder withCreated(@Nonnull String created) {
      return withCreated(TaskUtil.parseDate(created));
    }

    public TaskBuilder withType(@Nonnull TaskType type) {
      myType = type;
      return this;
    }

    public TaskBuilder withState(@Nullable TaskState state) {
      myState = state;
      return this;
    }

    public TaskBuilder withIcon(@Nullable Image icon) {
      myIcon = icon;
      return this;
    }

    @Nonnull
    @Override
    public String getId() {
      return myId;
    }

    @Nonnull
    @Override
    public String getSummary() {
      return mySummary;
    }

    @Nullable
    @Override
    public String getDescription() {
      return myDescription;
    }

    @Nonnull
    @Override
    public Comment[] getComments() {
      return myComments;
    }

    @Nonnull
    @Override
    public Image getIcon() {
      return myIcon == null? myRepository.getIcon() : myIcon;
    }

    @Nonnull
    @Override
    public TaskType getType() {
      return myType;
    }

    @Nullable
    @Override
    public TaskState getState() {
      return myState;
    }

    @Nullable
    @Override
    public Date getUpdated() {
      return myUpdated;
    }

    @Nullable
    @Override
    public Date getCreated() {
      return myCreated;
    }

    @Override
    public boolean isClosed() {
      return myClosed;
    }

    @Override
    public boolean isIssue() {
      return myIssue;
    }

    @Nullable
    @Override
    public String getIssueUrl() {
      return myIssueUrl;
    }

    @Nullable
    @Override
    public TaskRepository getRepository() {
      return myRepository;
    }
  }


}
