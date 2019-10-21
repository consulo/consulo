// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.messages.impl;

import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface MessageDeliveryListener {
  void messageDelivered(@Nonnull Topic<?> topic, @Nonnull String messageName, @Nonnull Object handler, long durationNanos);
}
