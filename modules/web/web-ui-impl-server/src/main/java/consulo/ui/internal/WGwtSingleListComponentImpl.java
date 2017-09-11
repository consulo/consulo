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

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.ListItemRender;
import consulo.ui.ListItemRenders;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.ValueComponent;
import consulo.ui.model.ListModel;
import consulo.web.gwt.shared.ui.state.combobox.UIComboBoxState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public abstract class WGwtSingleListComponentImpl<E> extends AbstractComponent implements ValueComponent<E> {
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();
  private List<ValueListener<E>> myValueListeners = new ArrayList<>();
  private ListModel<E> myModel;
  private int myIndex = -1;

  public WGwtSingleListComponentImpl(ListModel<E> model) {
    myModel = model;
  }

  protected abstract boolean needRenderNullValue();

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @NotNull
  //@Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  //@Override
  public void setRender(@NotNull ListItemRender<E> render) {
    myRender = render;
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    UIComboBoxState state = getState();
    state.myItems.clear();
    buildState(state.myItems);
  }

  protected void buildState(List<UIComboBoxState.Item> children) {
    // need render null value
    if (needRenderNullValue()) {
      renderItem(children, -1, null);
    }

    for (int i = 0; i < myModel.getSize(); i++) {
      E value = myModel.get(i);

      renderItem(children, i, value);
    }
  }

  private void renderItem(List<UIComboBoxState.Item> children, int i, @Nullable E e) {
    WGwtListItemPresentationImpl presentation = new WGwtListItemPresentationImpl();
    myRender.render(presentation, i, e);

    children.add(presentation.getItem());
  }

  @Override
  protected UIComboBoxState getState() {
    return (UIComboBoxState)super.getState();
  }

  @Override
  public void addValueListener(@NotNull ValueListener<E> valueListener) {
    myValueListeners.add(valueListener);
  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<E> valueListener) {
    myValueListeners.remove(valueListener);
  }

  @Nullable
  @Override
  public E getValue() {
    if (myIndex == -1) {
      return null;
    }
    return myModel.get(myIndex);
  }

  //@Override
  @RequiredUIAccess
  public void setValueByIndex(int index) {
    setValue(myModel.get(index));
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }

  @Override
  @RequiredUIAccess
  public void setValue(@Nullable E value) {
    setValueImpl(value);
  }

  public void setValueImpl(@Nullable E value) {
    if (value == null) {
      myIndex = -1;
    }
    else {
      final int i = myModel.indexOf(value);
      if (i == -1) {
        throw new IndexOutOfBoundsException();
      }
      myIndex = i;
    }

    final ValueEvent<E> event = new ValueEvent<>(this, value);
    for (ValueListener<E> valueListener : myValueListeners) {
      valueListener.valueChanged(event);
    }

    markAsDirty();
  }
}