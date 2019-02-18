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

import com.intellij.openapi.Disposable;
import com.vaadin.ui.AbstractComponent;
import consulo.ui.*;
import consulo.ui.web.internal.border.WebBorderBuilder;
import consulo.ui.model.ListModel;
import consulo.ui.shared.Size;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public abstract class WGwtSingleListComponentImpl<E> extends AbstractComponent implements ValueComponent<E>, VaadinWrapper {
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();
  private ListModel<E> myModel;

  public WGwtSingleListComponentImpl(ListModel<E> model) {
    myModel = model;
  }

  protected abstract boolean needRenderNullValue();

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Nonnull
  //@Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  //@Override
  public void setRender(@Nonnull ListItemRender<E> render) {
    myRender = render;
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    ComboBoxState state = getState();
    state.myItems.clear();
    buildState(state.myItems);

    WebBorderBuilder.fill(this, state.myBorderState);
  }

  protected void buildState(List<ComboBoxState.Item> children) {
    // need render null value
    if (needRenderNullValue()) {
      buildItem(children, -1, null);
    }

    for (int i = 0; i < myModel.getSize(); i++) {
      E value = myModel.get(i);

      buildItem(children, i, value);
    }
  }

  private void buildItem(List<ComboBoxState.Item> children, int i, @Nullable E e) {
    WGwtItemPresentationImpl presentation = new WGwtItemPresentationImpl();
    myRender.render(presentation, i, e);

    children.add(presentation.getItem());
  }

  @Override
  protected ComboBoxState getState() {
    return (ComboBoxState)super.getState();
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueListener<E> valueListener) {
    return dataObject().addListener(ValueListener.class, valueListener);
  }

  @javax.annotation.Nullable
  @Override
  public E getValue() {
    if (getState().mySelectedIndex == -1) {
      return null;
    }
    return myModel.get(getState().mySelectedIndex);
  }

  //@Override
  @RequiredUIAccess
  public void setValueByIndex(int index) {
    setValue(myModel.get(index));
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    getState().myHeight = size.getHeight();
    getState().myWidth = size.getWidth();
    markAsDirty();
  }

  @Override
  @RequiredUIAccess
  public void setValue(@Nullable E value, boolean fireEvents) {
    setValueImpl(value, fireEvents);
  }

  public void setValueImpl(@Nullable E value, boolean fireEvents) {
    if (value == null) {
      getState().mySelectedIndex = -1;
    }
    else {
      final int i = myModel.indexOf(value);
      if (i == -1) {
        throw new IndexOutOfBoundsException();
      }
      getState().mySelectedIndex = i;
    }

    if (fireEvents) {
      final ValueEvent<E> event = new ValueEvent<>(this, value);
      dataObject().getDispatcher(ValueListener.class).valueChanged(event);
    }

    markAsDirty();
  }
}