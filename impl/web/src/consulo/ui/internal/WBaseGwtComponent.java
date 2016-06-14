/*
 * Copyright 2013-2016 must-be.org
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

import com.google.web.bindery.autobean.shared.AutoBean;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.SmartList;
import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import consulo.web.servlet.ui.UISessionManager;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WBaseGwtComponent implements Component {
  private static final AtomicLong ourIndex = new AtomicLong();

  private long myId = ourIndex.incrementAndGet();
  private boolean myVisible = true;
  private boolean myEnabled = true;
  private Component myParent;

  private UIComponent myNotifyComponent;

  public long getId() {
    return myId;
  }

  @RequiredUIThread
  protected void markAsChanged() {
    UIAccess.assertIsUIThread();

    if (myNotifyComponent != null) {
      final HashMap<String, String> map = new HashMap<String, String>();
      getState(map);
      myNotifyComponent.setVariables(map);
    }
    else {
      final AutoBean<UIComponent> bean = UISessionManager.ourEventFactory.component();
      myNotifyComponent = bean.as();
      myNotifyComponent.setId(getId());

      final HashMap<String, String> map = new HashMap<String, String>();

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

  @Nullable
  @Override
  public Component getParentComponent() {
    return myParent;
  }

  protected void setParentComponent(Component parent) {
    myParent = parent;
  }

  public void registerComponent(TLongObjectHashMap<WBaseGwtComponent> map) {
    map.put(getId(), this);
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Override
  @RequiredUIThread
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
  @RequiredUIThread
  public void setEnabled(final boolean value) {
    if (myEnabled == value) {
      return;
    }

    myEnabled = value;

    markAsChanged();
  }

  public UIComponent convert(UIEventFactory factory) {
    AutoBean<UIComponent> bean = factory.component();

    UIComponent component = bean.as();
    component.setType(getClass().getName());
    component.setId(myId);

    Map<String, String> map = new HashMap<String, String>();
    getState(map);
    if (!map.isEmpty()) {
      component.setVariables(map);
    }

    List<UIComponent.Child> children = new SmartList<UIComponent.Child>();
    initChildren(factory, children);
    if (!children.isEmpty()) {
      component.setChildren(children);
    }
    return component;
  }

  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {

  }

  protected void getState(Map<String, String> map) {
    putIfNotDefault("visible", myVisible, true, map);
    putIfNotDefault("enabled", myEnabled, true, map);
  }

  public void invokeListeners(String type, Map<String, String> variables) {

  }

  public void visitChanges(List<UIComponent> components) {
    final UIComponent notifyComponent = getNotifyComponentAndClear();
    if (notifyComponent != null) {
      components.add(notifyComponent);
    }
  }

  protected <T> void putIfNotDefault(String key, T value, T defaultValue, Map<String, String> map) {
    if (!Comparing.equal(value, defaultValue)) {
      map.put(key, String.valueOf(value));
    }
  }
}
