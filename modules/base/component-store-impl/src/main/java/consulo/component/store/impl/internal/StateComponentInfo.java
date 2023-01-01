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
package consulo.component.store.impl.internal;

import consulo.component.ComponentManager;
import consulo.component.persist.*;
import consulo.container.PluginException;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.xml.serializer.JDOMExternalizable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class StateComponentInfo<T> {
  private static final Logger LOG = Logger.getInstance(StateComponentInfo.class);

  private static final String OPTION_WORKSPACE = "workspace";

  @Nullable
  public static <K> StateComponentInfo<K> build(@Nonnull K o, @Nullable ComponentManager project) {
    if (!(o instanceof PersistentStateComponent) && !(o instanceof JDOMExternalizable)) {
      return null;
    }
    PersistentStateComponent<?> stateComponent = null;
    State state = null;
    if (o instanceof PersistentStateComponent) {
      state = getStateSpec(o.getClass());
      stateComponent = (PersistentStateComponent<?>)o;
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
      return new StateComponentInfo<>((PersistentStateComponent<K>)stateComponent, state);
    }

    if (pluginId != null) {
      throw new PluginException("No @State annotation found in " + o.getClass().getName(), pluginId);
    }
    throw new RuntimeException("No @State annotation found in " + o.getClass().getName());
  }

  private static boolean isWorkspace(@Nullable Map<?, ?> options) {
    return options != null && Boolean.parseBoolean((String)options.get(OPTION_WORKSPACE));
  }

  @Nullable
  private static State getStateSpec(@Nonnull Class<?> aClass) {
    do {
      State stateSpec = aClass.getAnnotation(State.class);
      if (stateSpec != null) {
        return stateSpec;
      }
    }
    while ((aClass = aClass.getSuperclass()) != null);
    return null;
  }

  private PersistentStateComponent<T> myComponent;
  private State myState;

  public StateComponentInfo(PersistentStateComponent<T> component, State state) {
    myComponent = component;
    myState = state;
  }

  @Nonnull
  public PersistentStateComponent<T> getComponent() {
    return myComponent;
  }

  @Nonnull
  public String getName() {
    return myState.name();
  }

  @Nonnull
  public State getState() {
    return myState;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StateComponentInfo && ((StateComponentInfo)obj).getComponent().equals(((StateComponentInfo)obj).getComponent());
  }
}
