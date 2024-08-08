/*
 * Copyright 2013-2020 consulo.io
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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * from kotlin
 */
@Singleton
@State(name = "StatusBar", storages = @Storage(value = "ide.general.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class StatusBarWidgetSettings implements PersistentStateComponent<StatusBarWidgetSettings.StatusBarState> {
  public static class StatusBarState {
    public Map<String, Boolean> widgets = new LinkedHashMap<>();
  }

  public static StatusBarWidgetSettings getInstance() {
    return Application.get().getUnbindedInstance(StatusBarWidgetSettings.class);
  }

  private StatusBarState myState = new StatusBarState();

  public boolean isEnabled(StatusBarWidgetFactory factory) {
    Boolean state = myState.widgets.get(factory.getId());
    return state == Boolean.TRUE || factory.isEnabledByDefault();
  }

  public void setEnabled(StatusBarWidgetFactory factory, boolean newValue) {
    if (factory.isEnabledByDefault() == newValue) {
      myState.widgets.remove(factory.getId());
    }
    else {
      myState.widgets.put(factory.getId(), newValue);
    }
  }

  @Nullable
  @Override
  public StatusBarState getState() {
    return myState;
  }

  @Override
  public void loadState(StatusBarState state) {
    XmlSerializerUtil.copyBean(state, myState);
  }
}
