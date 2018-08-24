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

import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import com.intellij.util.pico.DefaultPicoContainer;
import consulo.annotations.DeprecationInfo;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import org.picocontainer.MutablePicoContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
@Deprecated
@DeprecationInfo("Hidden impl")
public class PicoInjectingContainer implements InjectingContainer {
  private MutablePicoContainer myContainer;

  public PicoInjectingContainer(@Nullable PicoInjectingContainer parent) {
    myContainer = new DefaultPicoContainer(parent == null ? null : parent.myContainer);
  }

  public PicoInjectingContainer(@Nonnull MutablePicoContainer container) {
    myContainer = container;
  }

  @Nonnull
  public MutablePicoContainer getContainer() {
    return myContainer;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getInstance(@Nonnull Class<T> clazz) {
    T instance = (T)myContainer.getComponentInstance(clazz);
    if (instance != null) {
      return instance;
    }
    throw new UnsupportedOperationException("Class " + clazz + " is not binded");
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUnbindedInstance(@Nonnull Class<T> clazz) {
    Object componentInstance = myContainer.getComponentInstance(clazz);
    if(componentInstance != null) {
      return (T)componentInstance;
    }
    CachingConstructorInjectionComponentAdapter adapter = new CachingConstructorInjectionComponentAdapter(clazz.getName(), clazz, null, true);
    return (T)adapter.getComponentInstance(myContainer);
  }

  @Nonnull
  @Override
  public InjectingContainerBuilder childBuilder() {
    return new PicoInjectingContainerBuilder(this);
  }

  @Override
  public void dispose() {
  }
}
