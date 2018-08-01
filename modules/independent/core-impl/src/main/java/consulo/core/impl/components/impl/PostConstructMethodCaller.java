/*
 * Copyright 2013-2018 consulo.io
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
package consulo.core.impl.components.impl;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import consulo.annotation.inject.PostConstruct;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author VISTALL
 * @since 2018-07-27
 */
class PostConstructMethodCaller implements TypeListener {
  static final PostConstructMethodCaller INSTANCE = new PostConstructMethodCaller();

  private Set<Object> duplicateCall = new CopyOnWriteArraySet<>();

  @Override
  public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
    encounter.register((InjectionListener<? super I>)instance -> {
      if(!duplicateCall.add(instance)) {
        System.out.println("twice call of " + instance.getClass().getSimpleName());
      }
      for (Method method : instance.getClass().getDeclaredMethods()) {
        if (method.isAnnotationPresent(PostConstruct.class)) {
          method.setAccessible(true);
          try {
            method.invoke(instance);
          }
          catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
          }
        }
      }
    });
  }
}
