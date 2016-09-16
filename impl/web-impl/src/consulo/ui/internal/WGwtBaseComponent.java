/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.SmartList;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.shared.Size;
import consulo.web.gwtUI.shared.UIComponent;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtBaseComponent implements Component {
  private static final AtomicLong ourIndex = new AtomicLong();

  private long myId = ourIndex.incrementAndGet();
  protected boolean myVisible = true;
  private boolean myEnabled = true;
  private Size mySize = Size.UNDEFINED;
  private Component myParent;

  private UIComponent myNotifyComponent;

  public long getId() {
    return myId;
  }

  @RequiredUIAccess
  protected void markAsChanged() {
    UIAccess.assertIsUIThread();

    if (myNotifyComponent != null) {
      final HashMap<String, Serializable> map = new HashMap<String, Serializable>();
      getState(map);
      myNotifyComponent.setVariables(map);
    }
    else {
      myNotifyComponent = new UIComponent();
      myNotifyComponent.setId(getId());

      final HashMap<String, Serializable> map = new HashMap<String, Serializable>();

      getState(map);

      myNotifyComponent.setVariables(map);
    }
  }

  @Nullable
  public UIComponent getNotifyComponentAndClear() {
    final UIComponent notifyComponent = myNotifyComponent;
    myNotifyComponent = null;
    return notifyComponent;
  }

  @Override
  public void dispose() {
  }

  @Override
  @RequiredUIAccess
  public void setSize(@NotNull Size size) {
    mySize = size;

    markAsChanged();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return myParent;
  }

  protected void setParentComponent(Component parent) {
    myParent = parent;
  }

  public void registerComponent(TLongObjectHashMap<WGwtBaseComponent> map) {
    map.put(getId(), this);
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Override
  @RequiredUIAccess
  public void setVisible(final boolean value) {
    if (myVisible == value) {
      return;
    }

    myVisible = value;

    markAsChanged();
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  @RequiredUIAccess
  public void setEnabled(final boolean value) {
    if (myEnabled == value) {
      return;
    }

    myEnabled = value;

    markAsChanged();
  }

  public UIComponent convert() {
    UIComponent component = new UIComponent();
    component.setType(getClass().getName());
    component.setId(myId);

    Map<String, Serializable> map = new HashMap<String, Serializable>();
    getState(map);
    if (!map.isEmpty()) {
      component.setVariables(map);
    }

    List<UIComponent.Child> children = new SmartList<UIComponent.Child>();
    initChildren(children);
    if (!children.isEmpty()) {
      component.setChildren(children);
    }
    return component;
  }

  protected void initChildren(List<UIComponent.Child> children) {

  }

  protected void getState(Map<String, Serializable> map) {
    putIfNotDefault("visible", myVisible, true, map);
    putIfNotDefault("enabled", myEnabled, true, map);
    if (mySize != Size.UNDEFINED) {
      map.put("componentSize", mySize);
    }
  }

  public void invokeListeners(String type, Map<String, Serializable> variables) {

  }

  public void visitChanges(List<UIComponent> components) {
    final UIComponent notifyComponent = getNotifyComponentAndClear();
    if (notifyComponent != null) {
      components.add(notifyComponent);
    }
  }

  protected <T> void putIfNotDefault(String key, T value, T defaultValue, Map<String, Serializable> map) {
    if (!Comparing.equal(value, defaultValue)) {
      map.put(key, String.valueOf(value));
    }
  }

  protected void putIfNotDefault(String key, boolean value, boolean defaultValue, Map<String, Serializable> map) {
    if (value != defaultValue) {
      map.put(key, value);
    }
  }
}
