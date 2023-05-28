/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.Tag;
import consulo.ui.ListBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.ListModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebListBoxImpl<E> extends VaadinComponentDelegate<WebListBoxImpl.Vaadin> implements ListBox<E> {
  @Tag("div")
  public class Vaadin extends com.vaadin.flow.component.listbox.ListBox implements FromVaadinComponentWrapper {

    @Nullable
    @Override
    public consulo.ui.Component toUIComponent() {
      return WebListBoxImpl.this;
    }
  }

  private final ListModel<E> myModel;

  public WebListBoxImpl(ListModel<E> model) {
    myModel = model;
  }

  @Nonnull
  @Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  @Override
  public void setRender(@Nonnull TextItemRender render) {

  }

  @Override
  public void setValueByIndex(int index) {

  }

  @Nullable
  @Override
  public E getValue() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setValue(Object value, boolean fireListeners) {

  }

  @Nonnull
  @Override
  public WebListBoxImpl.Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
