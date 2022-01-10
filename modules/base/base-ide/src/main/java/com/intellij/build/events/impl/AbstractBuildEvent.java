// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.events.impl;

import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.BuildEventsNls;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractBuildEvent implements BuildEvent {

  private final @Nonnull
  Object myEventId;
  private @Nullable Object myParentId;
  private final long myEventTime;
  private final @Nonnull
  @BuildEventsNls.Message String myMessage;
  private @Nullable @BuildEventsNls.Hint String myHint;
  private @Nullable @BuildEventsNls.Description String myDescription;

  public AbstractBuildEvent(@Nonnull Object eventId, @Nullable Object parentId, long eventTime, @Nonnull @BuildEventsNls.Message String message) {
    myEventId = eventId;
    myParentId = parentId;
    myEventTime = eventTime;
    myMessage = message;
  }

  @Override
  public @Nonnull
  Object getId() {
    return myEventId;
  }

  @Override
  public @Nullable Object getParentId() {
    return myParentId;
  }

  public void setParentId(@Nullable Object parentId) {
    myParentId = parentId;
  }

  @Override
  public long getEventTime() {
    return myEventTime;
  }

  @Override
  public @Nonnull
  @BuildEventsNls.Message String getMessage() {
    return myMessage;
  }

  @Override
  public @Nullable String getHint() {
    return myHint;
  }

  public void setHint(@Nullable @BuildEventsNls.Hint String hint) {
    myHint = hint;
  }

  @Override
  public @Nullable @BuildEventsNls.Description String getDescription() {
    return myDescription;
  }

  public void setDescription(@Nullable @BuildEventsNls.Description String description) {
    myDescription = description;
  }

  @NonNls
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
           "myEventId=" + myEventId +
           ", myParentId=" + myParentId +
           ", myEventTime=" + myEventTime +
           ", myMessage='" + myMessage + '\'' +
           ", myHint='" + myHint + '\'' +
           ", myDescription='" + myDescription + '\'' +
           '}';
  }
}
