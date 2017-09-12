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
package consulo.ui.internal;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBList;
import consulo.ui.ListBox;
import consulo.ui.ListItemRender;
import consulo.ui.ListItemRenders;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.ValueComponent;
import consulo.ui.migration.ToSwingWrapper;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public class DesktopListBoxImpl<E> implements ListBox<E>, ToSwingWrapper {
  private JBList<E> myList;
  private DesktopComboBoxModelWrapper<E> myModel;
  private ListItemRender<E> myRender = ListItemRenders.defaultRender();

  private JScrollPane myRootPanel;

  public DesktopListBoxImpl(ListModel<E> model) {
    myModel = new DesktopComboBoxModelWrapper<>(model);

    myList = new JBList<>(myModel);
    myList.setCellRenderer(new ColoredListCellRenderer<E>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList<? extends E> list, E value, int index, boolean selected, boolean hasFocus) {
        DesktopItemPresentationImpl<E> render = new DesktopItemPresentationImpl<>(this);
        myRender.render(render, index, value);
      }
    });
    myRootPanel = ScrollPaneFactory.createScrollPane(myList);
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
  public void setValueByIndex(int index) {
    myList.setSelectedIndex(index);
  }

  @RequiredUIAccess
  @Override
  public void setValue(E value, boolean fireEvents) {
    myList.setSelectedValue(value, true);
  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<E> valueListener) {
    myList.addListSelectionListener(new DesktopValueListenerAsListSelectionListener<>(this, myList, valueListener));
  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<E> valueListener) {
    myList.removeListSelectionListener(new DesktopValueListenerAsListSelectionListener<>(this, myList, valueListener));
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @Override
  public E getValue() {
    return myList.getSelectedValue();
  }

  @Override
  public boolean isVisible() {
    return myRootPanel.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myRootPanel.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myList.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myList.setEnabled(value);
  }

  @Nullable
  @Override
  public consulo.ui.Component getParentComponent() {
    return (consulo.ui.Component)myRootPanel.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }

  @NotNull
  @Override
  public Component toAWT() {
    return myRootPanel;
  }
}
