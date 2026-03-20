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
package consulo.component.impl.internal.inject;

import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.internal.inject.InjectingKey;
import consulo.component.internal.inject.RootInjectingContainerFactory;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
public class DefaultRootInjectingContainerFactory implements RootInjectingContainerFactory {
  private static final InjectingContainer ROOT = new InjectingContainer() {
    
    @Override
    public <T> T getInstance(Class<T> clazz) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> @Nullable T getInstanceIfCreated(Class<T> clazz) {
      throw new UnsupportedOperationException();
    }

    
    @Override
    public <T> T getUnbindedInstance(Class<T> clazz) {
      throw new UnsupportedOperationException();
    }

    
    @Override
    public <T> T getUnbindedInstance(Class<T> clazz, Type[] constructorTypes, Function<Object[], T> constructor) {
      throw new UnsupportedOperationException();
    }

    
    @Override
    public List<InjectingKey<?>> getKeys() {
      return Collections.emptyList();
    }

    
    @Override
    public InjectingContainerBuilder childBuilder() {
      return new DefaultInjectingContainerBuilder(null);
    }

    @Override
    public void dispose() {
    }
  };

  
  @Override
  public InjectingContainer getRoot() {
    return ROOT;
  }
}
