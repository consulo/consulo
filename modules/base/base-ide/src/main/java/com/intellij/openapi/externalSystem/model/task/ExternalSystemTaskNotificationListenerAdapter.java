package com.intellij.openapi.externalSystem.model.task;

import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 11/10/11 12:18 PM
 */
public abstract class ExternalSystemTaskNotificationListenerAdapter implements ExternalSystemTaskNotificationListener {

  @Nonnull
  public static final ExternalSystemTaskNotificationListener NULL_OBJECT = new ExternalSystemTaskNotificationListenerAdapter() { };

  @Override
  public void onQueued(@Nonnull ExternalSystemTaskId id) {
  }

  @Override
  public void onStart(@Nonnull ExternalSystemTaskId id) {
  }

  @Override
  public void onStatusChange(@Nonnull ExternalSystemTaskNotificationEvent event) {
  }

  @Override
  public void onTaskOutput(@Nonnull ExternalSystemTaskId id, @Nonnull String text, boolean stdOut) {
  }

  @Override
  public void onEnd(@Nonnull ExternalSystemTaskId id) {
  }

  @Override
  public void onSuccess(@Nonnull ExternalSystemTaskId id) {
  }

  @Override
  public void onFailure(@Nonnull ExternalSystemTaskId id, @Nonnull Exception e) {
  }
}
