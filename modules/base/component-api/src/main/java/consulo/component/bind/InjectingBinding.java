/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ComponentScope;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public interface InjectingBinding {
  Type[] EMPTY_TYPES = new Type[0];

  @Nonnull
  default String getApiClassName() {
    return getApiClass().getName();
  }

  @Nonnull
  Class getApiClass();

  @Nonnull
  Class getImplClass();

  @Nonnull
  Class getComponentAnnotationClass();

  @Nonnull
  ComponentScope getComponentScope();

  int getComponentProfiles();

  default int getParametersCount() {
    return getParameterTypes().length;
  }

  @Nonnull
  Type[] getParameterTypes();

  @Nonnull
  Object create(Object[] args);

  default boolean isLazy() {
    // used only for services
    return true;
  }
}
