/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.impl.internal.messagebus;

import consulo.component.bind.TopicBinding;
import consulo.component.bind.TopicMethod;
import consulo.component.internal.inject.TopicBindingLoader;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.lang.ObjectUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
public class TopicInvocationHandler<L> implements InvocationHandler, Function<Method, TopicMethod> {
  private static final Logger LOG = Logger.getInstance(TopicInvocationHandler.class);

  private final MessageBusImpl myMessageBus;
  private final Class<L> myTopicClass;

  private final TopicMethod[] myTopicMethods;
  private final Map<Method, TopicMethod> myMethodToTopicMethodCache;

  public TopicInvocationHandler(MessageBusImpl messageBus, Class<L> topicClass) {
    myMessageBus = messageBus;
    myTopicClass = topicClass;

    TopicBinding binding = TopicBindingLoader.INSTANCE.getBinding(topicClass.getName());

    if (binding != null) {
      myTopicMethods = binding.methods();
      myMethodToTopicMethodCache = new ConcurrentHashMap<>();
    }
    else {
      myTopicMethods = null;
      myMethodToTopicMethodCache = Map.of();
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass() == Object.class) {
      return EventDispatcher.handleObjectMethod(proxy, args, method.getName());
    }

    if (myTopicMethods != null) {
      TopicMethod topicMethod = myMethodToTopicMethodCache.computeIfAbsent(method, this);
      if (topicMethod != null) {
        myMessageBus.sendMessage(new TopicMethodMessage<>(myTopicClass, topicMethod, args));
        return ObjectUtil.NULL;
      }
    }

    return reflectionCall(method, args);
  }

  private Object reflectionCall(Method method, Object[] args) {
    myMessageBus.sendMessage(new ReflectionMessage<>(myTopicClass, method, args));
    return ObjectUtil.NULL;
  }

  @Override
  public TopicMethod apply(Method method) {
    for (TopicMethod topicMethod : myTopicMethods) {
      if (!topicMethod.getName().equals(topicMethod.getName())) {
        continue;
      }

      Type[] argumentTypes = topicMethod.getArgumentTypes();
      if (argumentTypes.length != method.getParameterCount()) {
        continue;
      }

      // zero count method
      if (argumentTypes.length == 0) {
        return topicMethod;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();

      for (int i = 0; i < parameterTypes.length; i++) {
        Type argumentType = argumentTypes[i];
        Class<?> parameterType = parameterTypes[i];
        if (argumentType == parameterType) {
          return topicMethod;
        }
      }
    }
    LOG.error("Can't find TopicMethod for " + method + ", class: " + myTopicClass);
    return null;
  }
}
