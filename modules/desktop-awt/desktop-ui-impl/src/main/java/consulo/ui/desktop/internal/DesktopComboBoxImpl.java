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
package consulo.ui.desktop.internal;

import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.model.ListModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxImpl<E> extends SwingComponentDelegate<ComboBoxWithWidePopup> implements ComboBox<E> {
  class MyComboBoxWithWidePopup extends ComboBoxWithWidePopup implements FromSwingComponentWrapper {
    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopComboBoxImpl.this;
    }
  }

  private ListModel<E> myModel;
  private TextItemRender<E> myRender = ListItemRenders.defaultRender();

  public DesktopComboBoxImpl(ListModel<E> model) {
    DesktopComboBoxModelWrapper wrapper = new DesktopComboBoxModelWrapper<>(model);
    myModel = model;

    myComponent = new MyComboBoxWithWidePopup();
    myComponent.setModel(wrapper);
    myComponent.setRenderer(new DesktopListRender<>(() -> myRender));
  }

  @Nonnull
  @Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  @Override
  public void setRender(@Nonnull TextItemRender<E> render) {
    myRender = render;
  }

  @Override
  public void setValueByIndex(int index) {
    myComponent.setSelectedIndex(index);
  }

  @RequiredUIAccess
  @Override
  public void setValue(E value, boolean fireListeners) {
    myComponent.setSelectedItem(value);
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<E> valueListener) {
    DesktopValueListenerAsItemListenerImpl<E> listener = new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, true);
    myComponent.addItemListener(listener);
    return () -> myComponent.removeItemListener(listener);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public E getValue() {
    return (E)myComponent.getSelectedItem();
  }
}
