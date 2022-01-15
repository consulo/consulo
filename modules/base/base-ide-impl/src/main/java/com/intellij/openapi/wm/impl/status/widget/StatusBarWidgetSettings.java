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
package com.intellij.openapi.wm.impl.status.widget;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.MemoryUsagePanel;
import com.intellij.util.xmlb.XmlSerializerUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * from kotlin
 */
@Singleton
@State(name = "StatusBar", storages = @Storage(value = "ide.general.xml"))
public class StatusBarWidgetSettings implements PersistentStateComponent<StatusBarWidgetSettings.StatusBarState> {
  public static class StatusBarState {
    public Map<String, Boolean> widgets = new LinkedHashMap<>();
  }

  public static StatusBarWidgetSettings getInstance() {
    return ServiceManager.getService(StatusBarWidgetSettings.class);
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

  @Override
  public void afterLoadState() {
    UISettings uiSettings = UISettings.getInstance();
    if(uiSettings.SHOW_MEMORY_INDICATOR) {
      uiSettings.SHOW_MEMORY_INDICATOR = false;

      myState.widgets.put(MemoryUsagePanel.WIDGET_ID, true);
    }
  }
}
