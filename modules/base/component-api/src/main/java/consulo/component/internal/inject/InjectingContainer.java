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
package consulo.component.internal.inject;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public interface InjectingContainer {
  static InjectingContainer root(ModuleLayer moduleLayer) {
    return ServiceLoader.load(moduleLayer, RootInjectingContainerFactory.class).findFirst().get().getRoot();
  }

  static InjectingContainer root(ClassLoader classLoader) {
    return ServiceLoader.load(RootInjectingContainerFactory.class, classLoader).findFirst().get().getRoot();
  }

  boolean LOG_INJECTING_PROBLEMS = Boolean.getBoolean("consulo.log.injecting.problems");

  
  <T> T getInstance(Class<T> clazz);

  @Nullable
  <T> T getInstanceIfCreated(Class<T> clazz);

  
  <T> T getUnbindedInstance(Class<T> clazz);

  
  <T> T getUnbindedInstance(Class<T> clazz, Type[] constructorTypes, Function<Object[], T> constructor);

  
  List<InjectingKey<?>> getKeys();

  
  InjectingContainerBuilder childBuilder();

  void dispose();
}
