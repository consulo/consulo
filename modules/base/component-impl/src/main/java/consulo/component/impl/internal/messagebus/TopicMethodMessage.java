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

import consulo.component.bind.TopicMethod;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
public class TopicMethodMessage<T> implements Message<T> {
  private final Class<T> myTopicClass;
  private final TopicMethod myTopicMethod;
  private final Object[] myArguments;

  public TopicMethodMessage(Class<T> topicClass, TopicMethod topicMethod, Object[] args) {
    myTopicClass = topicClass;
    myTopicMethod = topicMethod;
    myArguments = args;
  }

  @Nonnull
  @Override
  public Class<T> getTopicClass() {
    return myTopicClass;
  }

  @Nonnull
  @Override
  public String getMethodName() {
    return myTopicMethod.getName();
  }

  @Override
  public void invoke(T handler) throws Throwable {
    myTopicMethod.getInvoker().accept(handler, myArguments);
  }
}
