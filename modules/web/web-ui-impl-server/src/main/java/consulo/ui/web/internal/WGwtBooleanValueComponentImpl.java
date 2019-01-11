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
package consulo.ui.web.internal;

import com.intellij.openapi.Disposable;
import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.shared.Size;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxRpc;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtBooleanValueComponentImpl extends AbstractComponent implements ValueComponent<Boolean>, VaadinWrapper {
  private final CheckBoxRpc myRpc = value -> setValueImpl(value, true);

  public WGwtBooleanValueComponentImpl(boolean selected) {
    getState().myChecked = selected;

    registerRpc(myRpc);
  }

  @Override
  protected CheckBoxState getState() {
    return (CheckBoxState)super.getState();
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return getState().myChecked;
  }

  @Override
  @RequiredUIAccess
  public void setValue(@Nullable final Boolean value, boolean fireEvents) {
    UIAccess.assertIsUIThread();

    if (value == null) {
      throw new IllegalArgumentException();
    }

    if (getState().myChecked == value) {
      return;
    }

    setValueImpl(value, fireEvents);

    markAsDirty();
  }

  @RequiredUIAccess
  private void setValueImpl(@Nullable Boolean value, boolean fireEvents) {
    getState().myChecked = value;

    if (fireEvents) {
      dataObject().getDispatcher(ValueListener.class).valueChanged(new ValueEvent<>(this, value));
    }
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    return dataObject().addListener(ValueListener.class, valueListener);
  }

  @javax.annotation.Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }
}
