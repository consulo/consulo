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

import jakarta.inject.Provider;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public interface InjectingPoint<T> {
  
  InjectingPoint<T> to(T value);

  
  InjectingPoint<T> to(InjectingKey<? extends T> key);

  
  InjectingPoint<T> to(Provider<T> provider);

  
  default InjectingPoint<T> to(Class<? extends T> key) {
    return to(InjectingKey.of(key));
  }

  InjectingPoint<T> forceSingleton();


  InjectingPoint<T> factory(Function<Provider<T>, T> remap);

  
  InjectingPoint<T> injectListener(PostInjectListener<T> consumer);

  
  InjectingPoint<T> constructorParameterTypes(Type[] constructorParameterTypes);

  
  InjectingPoint<T> constructorFactory(Function<Object[], T> factory);
}
