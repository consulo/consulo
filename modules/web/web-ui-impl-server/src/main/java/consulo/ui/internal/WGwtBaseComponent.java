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
import com.intellij.util.BitUtil;
import com.intellij.util.SmartList;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.UIAccess;
import consulo.web.gwt.shared.UIComponent;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
@Deprecated
@DeprecationInfo("Old our Consulo <-> GWT transport")
public class WGwtBaseComponent implements Component {
  private static final AtomicLong ourIndex = new AtomicLong();

  protected static final int VARIABLES_CHANGED = 1 << 0;
  protected static final int CHILDREN_CHANGED = 1 << 1;

  protected static final int ALL = VARIABLES_CHANGED | CHILDREN_CHANGED;

  private long myId = ourIndex.incrementAndGet();
  protected boolean myVisible = true;
  private boolean myEnabled = true;
  private Size mySize = Size.UNDEFINED;
  private Component myParent;

  private long myChangeBits = 0;

  private long myEventBits = 0;

  public long getId() {
    return myId;
  }

  public void enableNotify(long mask) {
    myEventBits = BitUtil.set(myEventBits, mask, true);
  }

  public void disableNotify(long mask) {
    myEventBits = BitUtil.set(myEventBits, mask, false);
  }

  @RequiredUIAccess
  protected void markAsChanged() {
    markAsChanged(VARIABLES_CHANGED);
  }

  @RequiredUIAccess
  protected void markAsChanged(int mask) {
    UIAccess.assertIsUIThread();
    myChangeBits = BitUtil.set(myChangeBits, mask, true);
  }

  @Nullable
  public UIComponent getNotifyComponentAndClear() {
    if(myChangeBits != 0) {
      UIComponent convert = convert(myChangeBits);
      myChangeBits = 0;
      return convert;
    }
    return null;
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

  @NotNull
  public UIComponent convert() {
    return convert(ALL);
  }

  @NotNull
  public UIComponent convert(long bits) {
    UIComponent component = new UIComponent();
    component.setType(getClass().getName());
    component.setId(myId);

    if(BitUtil.isSet(bits, VARIABLES_CHANGED)) {
      Map<String, Object> map = new HashMap<>();
      getState(map);
      component.setVariables(map);
    }

    if(BitUtil.isSet(bits, CHILDREN_CHANGED)) {
      List<UIComponent.Child> children = new SmartList<>();
      initChildren(children);
      component.setChildren(children);
    }

    component.setEventBits(myEventBits);
    return component;
  }

  protected void initChildren(List<UIComponent.Child> children) {

  }

  protected void getState(Map<String, Object> map) {
    putIfNotDefault("visible", myVisible, true, map);
    putIfNotDefault("enabled", myEnabled, true, map);
    if (mySize != Size.UNDEFINED) {
      map.put("componentSize", mySize);
    }
  }

  @RequiredUIAccess
  public void invokeListeners(long type, Map<String, Object> variables) {

  }

  public void visitChanges(List<UIComponent> components) {
    final UIComponent notifyComponent = getNotifyComponentAndClear();
    if (notifyComponent != null) {
      components.add(notifyComponent);
    }
  }

  protected <T> void putIfNotDefault(String key, T value, T defaultValue, Map<String, Object> map) {
    if (!Comparing.equal(value, defaultValue)) {
      map.put(key, String.valueOf(value));
    }
  }

  protected void putIfNotDefault(String key, boolean value, boolean defaultValue, Map<String, Object> map) {
    if (value != defaultValue) {
      map.put(key, value);
    }
  }
}
