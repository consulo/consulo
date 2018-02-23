package com.intellij.util.messages;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public interface MessageBusListener<L> {
  @Nonnull
  Topic<L> getTopic();
  @Nonnull
  L getListener();
}
