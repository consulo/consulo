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
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @since 2018-07-27
 */
public class ComponentInitListener implements TypeListener {
  public static final ComponentInitListener INSTANCE = new ComponentInitListener();

  @Override
  public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
    encounter.register((InjectionListener<I>)injectee -> {
      Pair<Binding, ComponentManagerImpl> info = ScopeProvisionListener.INSTANCE.getComponentManager();

      Key<I> key = Key.get(type);
      if (info != null && info.getFirst().getKey().equals(key)) {
        info.getSecond()._setupComponent(key, injectee);
      }
    });
  }
}
