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
package consulo.component.bind;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
public final class TopicMethod {
  public static TopicMethod create(String name, Type[] args, BiConsumer<Object, Object[]> invoker) {
    return new TopicMethod(name, args, invoker);
  }

  private final String myName;
  private final Type[] myArgumentTypes;
  private final BiConsumer<Object, Object[]> myInvoker;

  private TopicMethod(String name, Type[] argumentTypes, BiConsumer<Object, Object[]> invoker) {
    myName = name;
    myArgumentTypes = argumentTypes;
    myInvoker = invoker;
  }

  public String getName() {
    return myName;
  }

  public Type[] getArgumentTypes() {
    return myArgumentTypes;
  }

  public BiConsumer<Object, Object[]> getInvoker() {
    return myInvoker;
  }
}
