/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author anna
 * @since 2008-06-26
 */
@Singleton
@State(name = "masterDetails", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class MasterDetailsStateService implements PersistentStateComponent<MasterDetailsStateService.States> {
  private final SkipDefaultValuesSerializationFilters mySerializationFilter = new SkipDefaultValuesSerializationFilters();
  private final Map<String, ComponentState> myStates = new HashMap<>();

  public static MasterDetailsStateService getInstance(@Nonnull ComponentManager project) {
    return project.getInstance(MasterDetailsStateService.class);
  }

  @Nullable
  public MasterDetailsState getComponentState(@Nonnull String key, Class<? extends MasterDetailsState> stateClass) {
    ComponentState state = myStates.get(key);
    if (state == null) return null;
    Element settings = state.mySettings;
    return settings != null ? XmlSerializer.deserialize(settings, stateClass) : null;
  }

  public void setComponentState(@Nonnull @NonNls String key, @Nonnull MasterDetailsState state) {
    Element element = XmlSerializer.serialize(state, mySerializationFilter);
    if (element == null) {
      myStates.remove(key);
    }
    else {
      ComponentState componentState = new ComponentState();
      componentState.myKey = key;
      componentState.mySettings = element;
      myStates.put(key, componentState);
    }
  }

  public States getState() {
    States states = new States();
    states.myStates.addAll(myStates.values());
    Collections.sort(states.getStates(), (o1, o2) -> o1.myKey.compareTo(o2.myKey));
    return states;
  }

  public void loadState(States states) {
    myStates.clear();
    for (ComponentState state : states.getStates()) {
      myStates.put(state.myKey, state);
    }
  }

  @Tag("state")
  public static class ComponentState {
    @Attribute("key")
    public String myKey;

    @Tag("settings")
    public Element mySettings;
  }

  public static class States {
    private List<ComponentState> myStates = new ArrayList<>();

    @Tag("states")
    @AbstractCollection(surroundWithTag = false)
    public List<ComponentState> getStates() {
      return myStates;
    }

    public void setStates(List<ComponentState> states) {
      myStates = states;
    }
  }
}
