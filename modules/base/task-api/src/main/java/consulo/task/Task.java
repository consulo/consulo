/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.task;

import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import org.jspecify.annotations.Nullable;
import java.util.Date;

/**
 * @author Dmitry Avdeev
 */
public abstract class Task {
  public static Task[] EMPTY_ARRAY = new Task[0];

  /**
   * Global unique task identifier, e.g. IDEA-00001. It's important that its format is consistent with
   * {@link TaskRepository#extractId(String)}, because otherwise task won't be updated on its activation.
   * Note that this ID is used to find issues and to compare them, so (ideally) it has to be unique.
   * <p>
   * In some cases task server doesn't offer such global ID (but, for instance, pair (project-name, per-project-id) instead) or it's not
   * what users want to see in UI (e.g. notorious <tt>id</tt> and <tt>iid</tt> in Gitlab). In this case you should generate artificial ID
   * for internal usage and implement {@link #getPresentableId()}.
   *
   * @return unique global ID as described
   * @see #getPresentableId()
   * @see TaskRepository#extractId(String)
   * @see TaskManager#activateTask(Task, boolean)
   */
  
  public abstract String getId();

  /**
   * @return ID in the form that is suitable for commit messages, dialogs, completion items, etc.
   */
  
  public String getPresentableId() {
    return getId();
  }

  /**
   * Short task description.
   *
   * @return description
   */
  
  public abstract String getSummary();

  public abstract @Nullable String getDescription();

  
  public abstract Comment[] getComments();

  
  public abstract Image getIcon();

  
  public abstract TaskType getType();

  public abstract @Nullable Date getUpdated();

  public abstract @Nullable Date getCreated();

  public abstract boolean isClosed();

  public @Nullable String getCustomIcon() {
    return null;
  }

  /**
   * @return true if bugtracking issue is associated
   */
  public abstract boolean isIssue();

  public abstract @Nullable String getIssueUrl();

  /**
   * @return null if no issue is associated
   * @see #isIssue()
   */
  public @Nullable TaskRepository getRepository() {
    return null;
  }

  public @Nullable TaskState getState() {
    return null;
  }

  @Override
  public final String toString() {
    String text;
    if (isIssue()) {
      text = getPresentableId() + ": " + getSummary();
    }
    else {
      text = getSummary();
    }
    return StringUtil.first(text, 60, true);
  }

  public String getPresentableName() {
    return toString();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof Task && ((Task)obj).getId().equals(getId());
  }

  @Override
  public final int hashCode() {
    return getId().hashCode();
  }

  /**
   * <b>Per-project</b> issue identifier. Default behavior is to extract project name from task's ID.
   * If your service doesn't provide issue ID in format <tt>PROJECT-123</tt> be sure to initialize it manually,
   * as it will be used to format commit messages.
   *
   * @return project-wide issue identifier
   * @see #getId()
   * @see TaskRepository#getCommitMessageFormat()
   */
  
  public String getNumber() {
    return extractNumberFromId(getId());
  }

  
  protected static String extractNumberFromId(String id) {
    int i = id.lastIndexOf('-');
    return i > 0 ? id.substring(i + 1) : id;
  }

  /**
   * Name of the project task belongs to. Default behavior is to extract project name from task's ID.
   * If your service doesn't provide issue ID in format <tt>PROJECT-123</tt> be sure to initialize it manually,
   * as it will be used to format commit messages.
   *
   * @return name of the project
   * @see #getId()
   * @see TaskRepository#getCommitMessageFormat()
   */
  public @Nullable String getProject() {
    return extractProjectFromId(getId());
  }

  protected static @Nullable String extractProjectFromId(String id) {
    int i = id.lastIndexOf('-');
    return i > 0 ? id.substring(0, i) : null;
  }
}
