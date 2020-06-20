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
package consulo.components.impl.stores;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.RoamingTypeDisabled;
import consulo.container.PluginException;
import consulo.container.plugin.ComponentConfig;
import consulo.container.plugin.PluginId;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class StateComponentInfo<T> {
  private static final Logger LOGGER = Logger.getInstance(StateComponentInfo.class);

  private static final String OPTION_WORKSPACE = "workspace";

  @Nullable
  public static <K> StateComponentInfo<K> build(@Nonnull K o, @Nullable Project project) {
    if (!(o instanceof PersistentStateComponent) && !(o instanceof JDOMExternalizable)) {
      return null;
    }
    PersistentStateComponent<?> stateComponent = null;
    State state = null;
    if (o instanceof PersistentStateComponent) {
      state = getStateSpec(o.getClass());
      stateComponent = (PersistentStateComponent<?>)o;

      if (project != null) {
        final ComponentConfig config = ((ComponentManagerImpl)project).getConfig(o.getClass());

        if (config != null && isWorkspace(config.options)) {
          LOGGER.warn("Marker 'workspace' is ignored for component: " + o.getClass().getName());
        }
      }
    }
    else if (o instanceof JDOMExternalizable) {
      state = getStateSpec(o.getClass());

      if (state == null) {
        RoamingType type = o instanceof RoamingTypeDisabled ? RoamingType.DISABLED : RoamingType.PER_USER;
        String name = ComponentManagerImpl.getComponentName(o);
        String file;
        if (project == null) {
          file = StoragePathMacros.DEFAULT_FILE;
        }
        else {
          final ComponentConfig config = ((ComponentManagerImpl)project).getConfig(o.getClass());
          assert config != null : "Couldn't find old storage for " + o.getClass().getName();

          final boolean workspace = isWorkspace(config.options);
          file = workspace ? StoragePathMacros.WORKSPACE_FILE : StoragePathMacros.DEFAULT_FILE;
        }

        state = new SimpleState(name, file, type);
      }

      stateComponent = new JDOMExternalizableWrapper((JDOMExternalizable)o);
    }

    PluginId pluginId = PluginManagerCore.getPluginId(o.getClass());
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
