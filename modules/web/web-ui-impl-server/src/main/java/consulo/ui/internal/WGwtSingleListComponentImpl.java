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

import consulo.ui.ListItemRender;
import consulo.ui.ListItemRenders;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.model.ListModel;
import consulo.web.gwt.shared.UIComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public abstract class WGwtSingleListComponentImpl<E> extends WGwtBaseComponent implements ValueComponent<E> {
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();
  private List<ValueListener<E>> myValueListeners = new ArrayList<ValueListener<E>>();
  private ListModel<E> myModel;
  private int myIndex = -1;

  public WGwtSingleListComponentImpl(ListModel<E> model) {
    myModel = model;
  }

  protected abstract boolean needRenderNullValue();

  @Override
  protected void getState(Map<String, Serializable> map) {
    super.getState(map);
    map.put("size", myModel.getSize());
    map.put("index", myIndex);
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
  protected void initChildren(List<UIComponent.Child> children) {
    if(needRenderNullValue()) {
      // need render null value
      renderItem(children, -1, null);
    }

    for (int i = 0; i < myModel.getSize(); i++) {
      E value = myModel.get(i);

      renderItem(children, i, value);
    }
  }

  private void renderItem(List<UIComponent.Child> children, int i, @Nullable E e) {
    WGwtListItemPresentationImpl render;
    render = new WGwtListItemPresentationImpl();
    myRender.render(render, i, e);

    final UIComponent component = render.getLayout().convert();
    component.setId(-1);

    final UIComponent.Child child = new UIComponent.Child();
    child.setComponent(component);

    children.add(child);
  }

  @Override
  public void invokeListeners(String type, Map<String, Serializable> variables) {
    if ("select".equals(type)) {
      myIndex = (Integer)variables.get("index");

      setValueImpl(myModel.get(myIndex));
    }
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

  @Override
  @RequiredUIAccess
  public void setValue(@Nullable E value) {
    setValueImpl(value);
    markAsChanged();
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

    final ValueEvent<E> event = new ValueEvent<E>(this, value);
    for (ValueListener<E> valueListener : myValueListeners) {
      valueListener.valueChanged(event);
    }
  }
}