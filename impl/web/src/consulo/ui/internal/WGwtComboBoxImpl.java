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

import consulo.ui.*;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtComboBoxImpl<E> extends WBaseGwtComponent implements ComboBox<E> {
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();
  private ListModel<E> myModel;
  private int myIndex = -1;

  public WGwtComboBoxImpl(ListModel<E> model) {
    myModel = model;
  }

  @Override
  protected void getState(Map<String, String> map) {
    super.getState(map);
    map.put("size", String.valueOf(myModel.getSize()));
    map.put("index", String.valueOf(myIndex));
  }

  @NotNull
  @Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  @Override
  public void setRender(@NotNull ListItemRender<E> render) {
    myRender = render;
  }

  @Override
  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {
    // need render null value
    renderItem(factory, children, -1, null);

    for (int i = 0; i < myModel.getSize(); i++) {
      E value = myModel.get(i);

      renderItem(factory, children, i, value);
    }
  }

  private void renderItem(UIEventFactory factory, List<UIComponent.Child> children, int i, @Nullable E e) {
    WGwtListItemPresentationImpl render;
    render = new WGwtListItemPresentationImpl();
    myRender.render(render, i, e);

    final UIComponent component = render.getLayout().convert(factory);
    component.setId(String.valueOf(i));

    final UIComponent.Child child = factory.componentChild().as();
    child.setComponent(component);

    children.add(child);
  }

  @Override
  public void addValueListener(@NotNull ValueListener<E> valueListener) {

  }

  @Override
  public void removeValueListener(@NotNull ValueListener<E> valueListener) {

  }

  @Nullable
  @Override
  public E getValue() {
    if (myIndex == -1) {
      return null;
    }
    return myModel.get(myIndex);
  }

  @Override
  @RequiredUIThread
  public void setValue(int index) {
    final E e = myModel.get(index);
    setValue(e);
  }

  @Override
  @RequiredUIThread
  public void setValue(@Nullable E value) {
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

    markAsChanged();
  }
}
