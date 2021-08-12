/*
 * Copyright 2013-2019 consulo.io
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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.web.internal.base.VaadinComponentDelegate;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebBooleanValueComponentBase<E extends VaadinBooleanValueComponentBase> extends VaadinComponentDelegate<E> implements ValueComponent<Boolean> {
  public WebBooleanValueComponentBase(boolean value) {
    getVaadinComponent().getState().myChecked = value;
  }

  @Nullable
  @Override
  public Boolean getValue() {
    return getVaadinComponent().getState().myChecked;
  }

  @RequiredUIAccess
  @Override
  public void setValue(Boolean value, boolean fireListeners) {
    UIAccess.assertIsUIThread();

    if (value == null) {
      throw new IllegalArgumentException();
    }

    if (getVaadinComponent().getState().myChecked == value) {
      return;
    }

    setValueImpl(value, fireListeners);

    getVaadinComponent().markAsDirty();
  }

  @RequiredUIAccess
  protected void setValueImpl(@Nullable Boolean value, boolean fireEvents) {
    getVaadinComponent().getState().myChecked = value;

    if (fireEvents) {
      getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent<>(this, value));
    }
  }
}
