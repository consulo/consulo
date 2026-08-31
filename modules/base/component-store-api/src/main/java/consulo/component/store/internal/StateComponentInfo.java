/*
 * Copyright 2013-2017 consulo.io
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
package consulo.component.store.internal;

import consulo.component.ComponentManager;
import consulo.component.internal.StateComponent;
import consulo.component.persist.*;
import consulo.container.PluginException;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.util.xml.serializer.JDOMExternalizable;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class StateComponentInfo {
  public static @Nullable StateComponentInfo build(Object o, @Nullable ComponentManager project) {
    if (!(o instanceof StateComponent) && !(o instanceof JDOMExternalizable)) {
      return null;
    }

    StateComponent stateComponent = null;
    State state = null;
    if (o instanceof StateComponent) {
      state = getStateSpec(o.getClass());
      stateComponent = (StateComponent)o;
    }
    else if (o instanceof JDOMExternalizable) {
      state = getStateSpec(o.getClass());

      if (state == null) {
        RoamingType type = o instanceof RoamingTypeDisabled ? RoamingType.DISABLED : RoamingType.DEFAULT;
        String file = StoragePathMacros.DEFAULT_FILE;
        state = new SimpleState(o.getClass().getName(), file, type);
      }

      stateComponent = new JDOMExternalizableWrapper((JDOMExternalizable)o);
    }

    PluginId pluginId = PluginManager.getPluginId(o.getClass());
    if (state != null && state.storages().length == 0) {
      if (pluginId == null) {
        throw new RuntimeException("No @State.storages() define in " + o.getClass().getName());
      }
      else {
        throw new PluginException("No @State.storages() define in " + o.getClass().getName(), pluginId);
      }
    }

    if (stateComponent != null && state != null) {
      return new StateComponentInfo(stateComponent, state);
    }

    if (pluginId != null) {
      throw new PluginException("No @State annotation found in " + o.getClass().getName(), pluginId);
    }
    throw new RuntimeException("No @State annotation found in " + o.getClass().getName());
  }

  private static @Nullable State getStateSpec(Class<?> aClass) {
    do {
      State stateSpec = aClass.getAnnotation(State.class);
      if (stateSpec != null) {
        return stateSpec;
      }
    }
    while ((aClass = aClass.getSuperclass()) != null);
    return null;
  }

  private final StateComponent myComponent;
  private final State myState;

  public StateComponentInfo(StateComponent component, State state) {
    myComponent = component;
    myState = state;
  }

  public StateComponent getComponent() {
    return myComponent;
  }

  /**
   * @return the state type argument declared by the component's state interface
   */
  public Class<?> getStateClass() {
    if (myComponent instanceof PersistentStateComponentAsync) {
      return ComponentSerializationUtil.getStateClass(myComponent.getClass(), PersistentStateComponentAsync.class);
    }
    return ComponentSerializationUtil.getStateClass(myComponent.getClass(), PersistentStateComponent.class);
  }

  public boolean isAsync() {
    return myComponent instanceof PersistentStateComponentAsync;
  }

  /**
   * Runs the synchronous post load callback. Asynchronous components expose it as a coroutine through
   * {@link PersistentStateComponentAsync#afterLoad(boolean)} instead, so this is a no-op for them.
   */
  public void afterLoad(boolean first) {
    if (myComponent instanceof PersistentStateComponent) {
      ((PersistentStateComponent<?>)myComponent).afterLoad(first);
    }
  }

  public String getName() {
    return myState.name();
  }

  public State getState() {
    return myState;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StateComponentInfo other && other.myComponent.equals(myComponent);
  }

  @Override
  public int hashCode() {
    return myComponent.hashCode();
  }
}
