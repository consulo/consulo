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
package consulo.web.internal.ui.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.provider.HasListDataView;
import consulo.ui.TextItemRender;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.MutableListModelListener;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class WebSingleListComponentBase<V, C extends Component & HasListDataView & HasValue & FromVaadinComponentWrapper> extends VaadinComponentDelegate<C> implements ValueComponent<V> {
  protected final ListModel<V> myModel;
  protected TextItemRender<V> myRender = (render, index, item) -> {
  };

  protected WebSingleListComponentBase(ListModel<V> model) {
    myModel = model;
    C component = toVaadinComponent();
    component.setItems(ContainerUtil.collect(model.iterator()));

    if (myModel instanceof MutableListModel mutableListModel) {
      mutableListModel.adddListener(new MutableListModelListener() {
        @Override
        public void itemAdded(@Nonnull Object item) {
          component.setItems(ContainerUtil.collect(model.iterator()));
        }

        @Override
        public void itemRemoved(@Nonnull Object item) {
          component.setItems(ContainerUtil.collect(model.iterator()));
        }
      });
    }
  }

  @Nonnull
  //@Override
  public ListModel<V> getListModel() {
    return myModel;
  }

  //@Override
  public void setRender(@Nonnull TextItemRender<V> render) {
    myRender = render;
  }

  @RequiredUIAccess
  public void setValueByIndex(int index) {
    setValue(myModel.get(index));
  }

  @Nullable
  @Override
  public V getValue() {
    return (V)toVaadinComponent().getValue();
  }

  @RequiredUIAccess
  @Override
  public void setValue(V value, boolean fireListeners) {
    getVaadinComponent().setValue(value);
  }
}
