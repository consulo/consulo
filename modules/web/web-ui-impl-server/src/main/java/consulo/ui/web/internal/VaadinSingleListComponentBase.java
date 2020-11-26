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
import consulo.ui.ListItemRenders;
import consulo.ui.ValueComponent;
import consulo.ui.model.ListModel;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.ui.web.internal.border.WebBorderBuilder;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class VaadinSingleListComponentBase<E> extends VaadinComponent {
  private TextItemRender<E> myRender = ListItemRenders.defaultRender();
  private ListModel<E> myModel;

  public void setModel(ListModel<E> model) {
    myModel = model;
  }

  public void setRender(TextItemRender<E> render) {
    myRender = render;
  }

  public ListModel<E> getModel() {
    return myModel;
  }

  public TextItemRender<E> getRender() {
    return myRender;
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    ComboBoxState state = getState();
    state.myItems.clear();
    buildState(state.myItems);

    WebBorderBuilder.fill(toUIComponent(), state.myBorderState);
  }

  protected abstract boolean needRenderNullValue();

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
    WebItemPresentationImpl presentation = new WebItemPresentationImpl();
    myRender.render(presentation, i, e);

    children.add(presentation.getItem());
  }

  public void setValueImpl(@Nullable E value, boolean fireEvents) {
    if (value == null) {
      getState().mySelectedIndex = -1;
    }
    else {
      final int i = myModel.indexOf(value);
      // -1 - value not found
      getState().mySelectedIndex = i;
    }

    if (fireEvents) {
      final ValueComponent.ValueEvent<E> event = new ValueComponent.ValueEvent<>(toUIComponent(), value);
      toUIComponent().getListenerDispatcher(ValueComponent.ValueListener.class).valueChanged(event);
    }

    markAsDirty();
  }

  @Override
  public ComboBoxState getState() {
    return (ComboBoxState)super.getState();
  }
}
