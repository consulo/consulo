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
package consulo.injecting;

import consulo.injecting.key.InjectingKey;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public interface InjectingContainer {
  static InjectingContainer root() {
    for (RootInjectingContainerFactory factory : ServiceLoader.load(RootInjectingContainerFactory.class, RootInjectingContainerFactory.class.getClassLoader())) {
      return factory.getRoot();
    }

    throw new UnsupportedOperationException("RootInjectingContainerFactory not found");
  }

  boolean LOG_INJECTING_PROBLEMS = Boolean.getBoolean("consulo.log.injecting.problems");

  @Nonnull
  <T> T getInstance(@Nonnull Class<T> clazz);

  @Nonnull
  <T> T getUnbindedInstance(@Nonnull Class<T> clazz);

  @Nonnull
  List<InjectingKey<?>> getKeys();

  @Nonnull
  InjectingContainerBuilder childBuilder();

  void dispose();
}
