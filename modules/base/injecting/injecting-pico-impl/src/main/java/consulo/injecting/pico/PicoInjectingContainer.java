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
package consulo.injecting.pico;

import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.injecting.key.InjectingKey;
import consulo.logging.Logger;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public class PicoInjectingContainer implements InjectingContainer {
  private static final Logger LOG = Logger.getInstance(PicoInjectingContainer.class);

  private final DefaultPicoContainer myContainer;
  private final List<InjectingKey<?>> myKeys;

  private final Set<Pair<Class, Class>> myGetInstanceWarningSet = new CopyOnWriteArraySet<>();

  public PicoInjectingContainer(@Nullable PicoInjectingContainer parent, int size) {
    myContainer = new DefaultPicoContainer(parent == null ? null : parent.myContainer);
    myKeys = new ArrayList<>(size);
  }

  void add(InjectingKey<?> key, PicoInjectingPoint point) {
    myKeys.add(key);
    myContainer.registerComponent(point.getAdapter());
  }

  @Nonnull
  @Override
  public List<InjectingKey<?>> getKeys() {
    return Collections.unmodifiableList(myKeys);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getInstance(@Nonnull Class<T> clazz) {
    Class<?> insideObjectCreation = GetInstanceValidator.insideObjectCreation();
    if (InjectingContainer.LOG_INJECTING_PROBLEMS && insideObjectCreation != null && myGetInstanceWarningSet.add(Pair.create(clazz, insideObjectCreation))) {
      LOG.warn("Calling #getInstance(" + clazz + ".class) inside object initialization. Use contructor injection instead. MainInjecting: " + insideObjectCreation);
    }

    T instance = (T)myContainer.getComponentInstance(clazz);
    if (instance != null) {
      return instance;
    }
    throw new UnsupportedOperationException("Class " + clazz + " is not binded");
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getInstanceIfCreated(@Nonnull Class<T> clazz) {
    Class<?> insideObjectCreation = GetInstanceValidator.insideObjectCreation();
    if (InjectingContainer.LOG_INJECTING_PROBLEMS && insideObjectCreation != null && myGetInstanceWarningSet.add(Pair.create(clazz, insideObjectCreation))) {
      LOG.warn("Calling #getInstance(" + clazz + ".class) inside object initialization. Use contructor injection instead. MainInjecting: " + insideObjectCreation);
    }

    return (T)myContainer.getComponentInstanceIfCreated(clazz);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUnbindedInstance(@Nonnull Class<T> clazz) {
    ConstructorInjectionComponentAdapter adapter = new ConstructorInjectionComponentAdapter(clazz.getName(), clazz);
    return (T)adapter.getComponentInstance(myContainer);
  }

  @Nonnull
  @Override
  public InjectingContainerBuilder childBuilder() {
    return new PicoInjectingContainerBuilder(this);
  }

  @Override
  public void dispose() {
    myKeys.clear();
    myGetInstanceWarningSet.clear();
    myContainer.dispose();
  }
}
