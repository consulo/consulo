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

import com.intellij.util.SmartList;
import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxRpc;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtBooleanValueComponentImpl extends AbstractComponent implements ValueComponent<Boolean> {
  private List<ValueComponent.ValueListener<Boolean>> myValueListeners = new SmartList<>();

  private final CheckBoxRpc myRpc = value -> setValueImpl(value);

  public WGwtBooleanValueComponentImpl(boolean selected) {
    getState().myChecked = selected;

    registerRpc(myRpc);
  }

  @Override
  protected CheckBoxState getState() {
    return (CheckBoxState)super.getState();
  }

  @NotNull
  @Override
  public Boolean getValue() {
    return getState().myChecked;
  }

  @Override
  @RequiredUIAccess
  public void setValue(@Nullable final Boolean value) {
    UIAccess.assertIsUIThread();

    if (value == null) {
      throw new IllegalArgumentException();
    }

    if (getState().myChecked == value) {
      return;
    }

    setValueImpl(value);

    markAsDirty();
  }

  private void setValueImpl(@Nullable Boolean value) {
    getState().myChecked = value;

    for (ValueListener<Boolean> valueListener : myValueListeners) {
      valueListener.valueChanged(new ValueEvent<>(this, value));
    }
  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    myValueListeners.add(valueListener);
  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    myValueListeners.remove(valueListener);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}
