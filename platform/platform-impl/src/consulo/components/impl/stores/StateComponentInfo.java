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

import com.intellij.diagnostic.PluginException;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.components.impl.stores.DirectoryStorageData;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.RoamingTypeDisabled;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class StateComponentInfo<T> {
  private static final String DEFAULT_APPLICATION_STORAGE_SPEC = StoragePathMacros.APP_CONFIG + "/other" + DirectoryStorageData.DEFAULT_EXT;

  @NonNls
  private static final String OPTION_WORKSPACE = "workspace";

  @Nullable
  public static <K> StateComponentInfo<K> of(@NotNull Object o, @Nullable Project project) {
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
        RoamingType type = o instanceof RoamingTypeDisabled ? RoamingType.DISABLED : RoamingType.PER_USER;
        String name = ComponentManagerImpl.getComponentName(o);
        String file;
        if (project == null) {
          file = DEFAULT_APPLICATION_STORAGE_SPEC;
        }
        else {
          final ComponentConfig config = ((ComponentManagerImpl)project).getConfig(o.getClass());
          assert config != null : "Couldn't find old storage for " + o.getClass().getName();

          final boolean workspace = isWorkspace(config.options);
          file = workspace ? StoragePathMacros.WORKSPACE_FILE : StoragePathMacros.PROJECT_FILE;
        }

        state = new SimpleState(name, file, type);
      }

      stateComponent = new JDOMExternalizableWrapper((JDOMExternalizable)o);
    }

    PluginId pluginId = PluginManagerCore.getPluginId(o.getClass());
    if (state != null && state.storages().length == 0) {
      if(pluginId == null) {
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
  private static State getStateSpec(@NotNull Class<?> aClass) {
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

  @NotNull
  public PersistentStateComponent<T> getComponent() {
    return myComponent;
  }

  @NotNull
  public String getName() {
    return myState.name();
  }

  @NotNull
  public State getState() {
    return myState;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StateComponentInfo && ((StateComponentInfo)obj).getComponent().equals(((StateComponentInfo)obj).getComponent());
  }
}
