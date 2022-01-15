// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.util.messages.impl;

import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;

import java.lang.reflect.Method;

public final class Message {
  private final Topic myTopic;
  private final Method myListenerMethod;
  private final Object[] myArgs;

  public Message(@Nonnull Topic topic, @Nonnull Method listenerMethod, Object[] args) {
    myTopic = topic;
    listenerMethod.setAccessible(true);
    myListenerMethod = listenerMethod;
    myArgs = args;
  }

  @Nonnull
  public Topic getTopic() {
    return myTopic;
  }

  @Nonnull
  public Method getListenerMethod() {
    return myListenerMethod;
  }

  public Object[] getArgs() {
    return myArgs;
  }

  @Override
  public String toString() {
    return myTopic + ":" + myListenerMethod.getName();
  }
}
