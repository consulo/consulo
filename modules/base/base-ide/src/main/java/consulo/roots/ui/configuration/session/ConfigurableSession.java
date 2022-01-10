/*
 * Copyright 2013-2021 consulo.io
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
package consulo.roots.ui.configuration.session;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import consulo.roots.ui.configuration.session.internal.ConfigurableSessionImpl;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 17/04/2021
 */
public interface ConfigurableSession {
  @RequiredUIAccess
  static ConfigurableSession get() {
    return ConfigurableSessionImpl.get();
  }

  /**
   * @return default or frame project
   */
  @Nonnull
  Project getProject();

  @Nonnull
  <T extends PersistentStateComponent<?>> T getOrCopy(@Nonnull ComponentManager componentManager, @Nonnull Class<T> clazz);

  /**
   * Can be disposable
   */
  @Nonnull
  <T> T get(@Nonnull Class<T> key, @Nonnull Function<Project, T> factory);

  <T extends EventListener> void addListener(@Nonnull Class<T> listenerClass, @Nonnull T listener);

  @Nonnull
  <T extends EventListener> T getListenerMulticaster(@Nonnull Class<T> listenerClass);
}
