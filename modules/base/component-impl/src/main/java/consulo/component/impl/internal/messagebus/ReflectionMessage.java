// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.component.impl.internal.messagebus;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public final class ReflectionMessage<T> implements Message<T> {
  private final Class<T> myTopicClass;
  private final Method myListenerMethod;
  private final Object[] myArgs;

  public ReflectionMessage(@Nonnull Class<T> topicClass, @Nonnull Method listenerMethod, Object[] args) {
    myTopicClass = topicClass;
    listenerMethod.setAccessible(true);
    myListenerMethod = listenerMethod;
    myArgs = args;
  }

  @Override
  @Nonnull
  public Class<T> getTopicClass() {
    return myTopicClass;
  }

  @Nonnull
  @Override
  public String getMethodName() {
    return myListenerMethod.getName();
  }

  @Override
  public void invoke(T handler) throws Throwable {
    myListenerMethod.invoke(handler, myArgs);
  }

  @Override
  public String toString() {
    return myTopicClass.getName() + ":" + myListenerMethod.getName();
  }
}
