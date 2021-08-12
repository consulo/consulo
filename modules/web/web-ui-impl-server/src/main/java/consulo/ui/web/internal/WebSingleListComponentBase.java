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

import consulo.ui.TextItemRender;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.ListModel;
import consulo.ui.web.internal.base.VaadinComponentDelegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebSingleListComponentBase<E, V extends VaadinSingleListComponentBase<E>> extends VaadinComponentDelegate<V> implements ValueComponent<E> {
  protected WebSingleListComponentBase(ListModel<E> model) {
    getVaadinComponent().setModel(model);
  }

  @Nonnull
  //@Override
  public ListModel<E> getListModel() {
    return getVaadinComponent().getModel();
  }

  //@Override
  public void setRender(@Nonnull TextItemRender<E> render) {
    getVaadinComponent().setRender(render);
  }

  @RequiredUIAccess
  public void setValueByIndex(int index) {
    setValue(getVaadinComponent().getModel().get(index));
  }

  @Nullable
  @Override
  public E getValue() {
    V vaadinComponent = getVaadinComponent();

    if (vaadinComponent.getState().mySelectedIndex == -1) {
      return null;
    }
    return vaadinComponent.getModel().get(vaadinComponent.getState().mySelectedIndex);
  }

  @RequiredUIAccess
  @Override
  public void setValue(E value, boolean fireListeners) {
    getVaadinComponent().setValueImpl(value, fireListeners);
  }
}
