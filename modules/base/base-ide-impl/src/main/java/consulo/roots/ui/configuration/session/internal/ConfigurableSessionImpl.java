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
package consulo.roots.ui.configuration.session.internal;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.util.EventDispatcher;
import consulo.disposer.Disposable;
import consulo.roots.ui.configuration.session.ConfigurableSession;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.EventListener;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 11/04/2021
 */
public final class ConfigurableSessionImpl implements Disposable, ConfigurableSession {
  private final Map<ConfigurableComponentKey, PersistentStateComponent<?>> myStateInstances = new ConcurrentHashMap<>();
  private final Map<Class, Object> myInstances = new ConcurrentHashMap<>();
  private final Map<Class, EventDispatcher> myListeners = new ConcurrentHashMap<>();

  private static ConfigurableSessionImpl ourCurrentSession;

  @Nonnull
  @RequiredUIAccess
  public static ConfigurableSessionImpl get() {
    UIAccess.assertIsUIThread();
    return Objects.requireNonNull(ourCurrentSession, "Session is not initialized");
  }

  private final Project myProject;

  public ConfigurableSessionImpl(Project project) {
    myProject = project;

    if (ourCurrentSession != null) {
      throw new IllegalArgumentException("already initialized");
    }

    ourCurrentSession = this;
  }

  @RequiredUIAccess
  public void commit() {
    commitImpl();
  }

  @RequiredUIAccess
  public void drop() {
    ourCurrentSession = null;

    disposeWithTree();
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends PersistentStateComponent<?>> T getOrCopy(@Nonnull ComponentManager componentManager, @Nonnull Class<T> clazz) {
    return (T)myStateInstances.computeIfAbsent(new ConfigurableComponentKey(componentManager, clazz), key -> {
      PersistentStateComponent<?> originalInstance = componentManager.getInstance(key.serviceKey());
      Object originalState = originalInstance.getState();

      PersistentStateComponent copyInstance = componentManager.getInjectingContainer().getUnbindedInstance(key.serviceKey());
      copyInstance.loadState(originalState);
      return copyInstance;
    });
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(@Nonnull Class<T> key, @Nonnull Function<Project, T> factory) {
    return (T)myInstances.computeIfAbsent(key, it -> factory.apply(myProject));
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T extends EventListener> T getListenerMulticaster(@Nonnull Class<T> listenerClass) {
    return (T)myListeners.computeIfAbsent(listenerClass, EventDispatcher::create).getMulticaster();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends EventListener> void addListener(@Nonnull Class<T> listenerClass, @Nonnull T listener) {
    myListeners.computeIfAbsent(listenerClass, EventDispatcher::create).addListener(listener);
  }

  @SuppressWarnings("unchecked")
  private void commitImpl() {
    for (Map.Entry<ConfigurableComponentKey, PersistentStateComponent<?>> entry : myStateInstances.entrySet()) {
      ConfigurableComponentKey key = entry.getKey();
      PersistentStateComponent<?> value = entry.getValue();

      Class<? extends PersistentStateComponent<?>> serviceKey = key.serviceKey();
      ComponentManager component = key.component();

      PersistentStateComponent originalInstance = component.getInstance(serviceKey);

      Object copyState = value.getState();

      originalInstance.loadState(copyState);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void dispose() {
    for (PersistentStateComponent<?> component : myStateInstances.values()) {
      if (component instanceof Disposable disposable) {
        disposable.disposeWithTree();
      }
    }

    myStateInstances.clear();
  }
}
