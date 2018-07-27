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
package consulo.core.impl.components.impl;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * @author VISTALL
 * @since 2018-07-27
 */
class ComponentManagerScope implements Scope {
  private ComponentManagerImpl myComponentManager;

  public ComponentManagerScope(ComponentManagerImpl componentManager) {
    myComponentManager = componentManager;
  }

  public ComponentManagerImpl getComponentManager() {
    return myComponentManager;
  }

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    return unscoped;
  }

  @Override
  public String toString() {
    return myComponentManager.toString();
  }
}
