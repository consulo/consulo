/*
 * Copyright 2013-2019 consulo.io
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

import consulo.component.persist.PersistentStateComponentAsync;

/**
 * @author VISTALL
 * @since 2019-11-16
 */
class DefaultComponentParameter<T> implements Parameter<T> {
  public static final Parameter DEFAULT = new DefaultComponentParameter();

  @Override
  public T resolveInstance(InstanceContainer container, ComponentAdapter<T> adapter, Class<? super T> expectedType) {
    ComponentAdapter<T> targetAdapter = container.getComponentAdapter(expectedType);
    if (targetAdapter != null) {
      Class<?> implClass = targetAdapter.getComponentImplClassIfCheap();
      if (implClass != null && PersistentStateComponentAsync.class.isAssignableFrom(implClass)) {
        throw new AsyncInjectionNotSupportedException(adapter.getComponentImplClass(), implClass);
      }
    }
    return container.getComponentInstance(expectedType);
  }

  /**
   * Deliberately unchanged for async state components: reporting them as unresolvable would make
   * {@code getGreediestSatisfiableConstructor} silently pick a different constructor instead of reporting the
   * problem. The rejection belongs in {@link #resolveInstance}, where it fails loudly.
   */
  @Override
  public boolean isResolvable(InstanceContainer container, ComponentAdapter<T> adapter, Class<? super T> expectedType) {
    return container.getComponentAdapter(expectedType) != null;
  }
}
