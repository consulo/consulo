// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.impl.internal.messagebus;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface MessageDeliveryListener {
  void messageDelivered(@Nonnull Class<?> topic, @Nonnull String messageName, @Nonnull Object handler, long durationNanos);
}
