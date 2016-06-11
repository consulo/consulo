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
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import consulo.web.servlet.ui.UISessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WBaseGwtComponent implements Component {
  private String myId = UUID.randomUUID().toString();
  private boolean myVisible = true;
  private boolean myEnabled = true;
  private Component myParent;

  private UIComponent myNotifyComponent;

  public String getId() {
    return myId;
  }

  @RequiredUIThread
  protected void makeChange(@NotNull Consumer<UIComponent> consumer) {
    UIAccess.assertIsUIThread();

    if (myNotifyComponent != null) {
      consumer.consume(myNotifyComponent);
    }
    else {
      final AutoBean<UIComponent> bean = UISessionManager.ourEventFactory.component();
      myNotifyComponent = bean.as();
      myNotifyComponent.setId(getId());

      myNotifyComponent.setVariables(new HashMap<String, String>());

      consumer.consume(myNotifyComponent);
    }
  }

  @Nullable
  public UIComponent getNotifyComponent() {
    final UIComponent notifyComponent = myNotifyComponent;
    myNotifyComponent = null;
    return notifyComponent;
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return myParent;
  }

  protected void setParentComponent(Component parent) {
    myParent = parent;
  }

  public void registerComponent(Map<String, WBaseGwtComponent> map) {
    map.put(getId(), this);
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Override
  @RequiredUIThread
  public void setVisible(boolean value) {
    myVisible = value;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  @RequiredUIThread
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public UIComponent convert(UIEventFactory factory) {
    AutoBean<UIComponent> bean = factory.component();

    UIComponent component = bean.as();
    component.setType(getClass().getName());
    component.setId(myId);

    Map<String, String> map = new HashMap<String, String>();
    initVariables(map);
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

  protected void initVariables(Map<String, String> map) {
    map.put("visible", String.valueOf(myVisible));
  }

  public void invokeListeners(Map<String, String> variables) {

  }

  public void visitChanges(List<UIComponent> components) {
    final UIComponent notifyComponent = getNotifyComponent();
    if(notifyComponent != null) {
      components.add(notifyComponent);
    }
  }
}
