// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build.events.impl;

import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.event.MessageEventResult;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class MessageEventImpl extends AbstractBuildEvent implements MessageEvent {

  @Nonnull
  private final Kind myKind;
  @Nonnull
  private final NotificationGroup myGroup;

  public MessageEventImpl(@Nonnull Object parentId,
                          @Nonnull Kind kind,
                          @Nonnull NotificationGroup group,
                          @Nonnull @BuildEventsNls.Message String message,
                          @Nullable @BuildEventsNls.Description String detailedMessage) {
    super(new Object(), parentId, System.currentTimeMillis(), message);
    myKind = kind;
    myGroup = group;
    setDescription(detailedMessage);
  }

  @Override
  public final void setDescription(@Nullable @BuildEventsNls.Description String description) {
    super.setDescription(description);
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @Nonnull
  @Override
  public NotificationGroup getGroup() {
    return myGroup;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@Nonnull Project project) {
    return null;
  }

  @Override
  public MessageEventResult getResult() {
    return new MessageEventResult() {
      @Override
      public Kind getKind() {
        return myKind;
      }

      @Override
      public String getDetails() {
        return getDescription();
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessageEventImpl event = (MessageEventImpl)o;
    return Objects.equals(getMessage(), event.getMessage()) && Objects.equals(getDescription(), event.getDescription()) && Objects.equals(myGroup, event.myGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myGroup, getMessage());
  }
}
