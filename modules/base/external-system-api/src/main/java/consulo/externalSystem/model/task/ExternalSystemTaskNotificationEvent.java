package consulo.externalSystem.model.task;

import jakarta.annotation.Nonnull;
import java.io.Serializable;

/**
 * Encapsulates information about processing state change of the {@link #getId() target task}.
 *
 * @author Denis Zhdanov
 * @since 11/10/11 9:19 AM
 */
public class ExternalSystemTaskNotificationEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private final ExternalSystemTaskId myId;
  private final String myDescription;

  public ExternalSystemTaskNotificationEvent(@Nonnull ExternalSystemTaskId id, @Nonnull String description) {
    myId = id;
    myDescription = description;
  }

  @Nonnull
  public ExternalSystemTaskId getId() {
    return myId;
  }

  @Nonnull
  public String getDescription() {
    return myDescription;
  }

  @Override
  public int hashCode() {
    return 31 * myDescription.hashCode() + myId.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalSystemTaskNotificationEvent that = (ExternalSystemTaskNotificationEvent)o;
    return myId.equals(that.myId) && myDescription.equals(that.myDescription);
  }

  @Override
  public String toString() {
    return myId + "-" + myDescription;
  }
}
