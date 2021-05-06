/*
 * Copyright 2013-2017 consulo.io
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

import com.intellij.ui.components.JBList;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.model.ListModel;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
class DesktopListBoxImpl<E> extends SwingComponentDelegate<JBList<E>> implements ListBox<E> {
  class MyJBList<T> extends JBList<T> implements FromSwingComponentWrapper {
    MyJBList(@Nonnull javax.swing.ListModel<T> dataModel) {
      super(dataModel);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopListBoxImpl.this;
    }
  }

  private TextItemRender<E> myRender = ListItemRenders.defaultRender();

  private ListModel<E> myModel;

  public DesktopListBoxImpl(ListModel<E> model) {
    myModel = model;
    DesktopComboBoxModelWrapper<E> wrapper = new DesktopComboBoxModelWrapper<>(model);

    myComponent = new MyJBList<>(wrapper);
    myComponent.setCellRenderer(new DesktopListRender<>(() -> myRender));
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
    myComponent.setSelectedValue(value, true);
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<E> valueListener) {
    DesktopValueListenerAsListSelectionListener<E> listener = new DesktopValueListenerAsListSelectionListener<>(this, myComponent, valueListener);
    myComponent.addListSelectionListener(listener);
    return () -> myComponent.removeListSelectionListener(listener);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  @Override
  public E getValue() {
    return myComponent.getSelectedValue();
  }
}
