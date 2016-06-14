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

import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.ui.ColoredListCellRendererWrapper2;
import consulo.ui.*;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxImpl<E> extends ComboBoxWithWidePopup implements ComboBox<E> {
  private DesktopComboBoxModelWrapper<E> myModel;
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();

  public DesktopComboBoxImpl(ListModel<E> model) {
    myModel = new DesktopComboBoxModelWrapper<E>(model);

    setModel(myModel);
    setRenderer(new ColoredListCellRendererWrapper2<E>() {
      @Override
      protected void doCustomize(JList list, E value, int index, boolean selected, boolean hasFocus) {
        DesktopListItemPresentationImpl<E> render = new DesktopListItemPresentationImpl<E>(this);
        myRender.render(render, index, value);
      }
    });
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
  public void setValue(int index) {
    setSelectedIndex(index);
  }

  @Override
  public void setValue(@NotNull E value) {
    setSelectedItem(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Override
  public void dispose() {

  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<E> valueListener) {
    addItemListener(new DesktopValueListenerAsItemListenerImpl<E>(valueListener, true));
  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<E> valueListener) {
    removeItemListener(new DesktopValueListenerAsItemListenerImpl<E>(valueListener, true));
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public E getValue() {
    return (E)getSelectedItem();
  }
}
