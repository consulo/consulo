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

import com.google.inject.Binding;
import com.google.inject.Scope;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.ProvisionListener;

/**
 * @author VISTALL
 * @since 2018-07-27
 */
class ScopeProvisionListener implements ProvisionListener {
  public static ScopeProvisionListener INSTANCE = new ScopeProvisionListener();

  @Override
  public <T> void onProvision(ProvisionInvocation<T> provision) {
    Binding<T> binding = provision.getBinding();

    ComponentManagerImpl manager = binding.acceptScopingVisitor(new DefaultBindingScopingVisitor<ComponentManagerImpl>() {
      @Override
      public ComponentManagerImpl visitScope(Scope scope) {
        return scope instanceof ComponentManagerScope ? ((ComponentManagerScope)scope).getComponentManager() : null;
      }
    });

    if (manager == null) {
      return;
    }

    T value = provision.provision();

    manager._setupComponent(value);
  }
}
