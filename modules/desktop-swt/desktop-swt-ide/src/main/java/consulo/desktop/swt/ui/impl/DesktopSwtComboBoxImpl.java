/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.ui.ComboBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.ListModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtComboBoxImpl<E> extends SWTComponentDelegate<CCombo> implements ComboBox<E> {
  private TextItemRender<E> myRender = (render, index, item) -> {
    if (item == null) {
      render.append("");
    }
    else {
      render.append(item.toString());
    }
  };

  private final ListModel<E> myModel;

  private int mySelectedIndex = 0;

  public DesktopSwtComboBoxImpl(ListModel<E> model) {
    myModel = model;
  }

  @Override
  protected CCombo createSWT(Composite parent) {
    return new CCombo(parent, SWT.BORDER | SWT.READ_ONLY);
  }

  @Override
  protected void initialize(CCombo component) {
    List<String> items = new ArrayList<>();

    for (int i = 0; i < myModel.getSize(); i++) {
      E element = myModel.get(i);

      DesktopSwtTextItemPresentation presentation = new DesktopSwtTextItemPresentation();

      myRender.render(presentation, i, element);

      items.add(presentation.toString());
    }

    component.setItems(items.toArray(String[]::new));

    component.select(mySelectedIndex);

    component.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        mySelectedIndex = component.getSelectionIndex();
      }
    });
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
    mySelectedIndex = index;

    if(myComponent != null) {
      myComponent.select(index);
    }
  }

  @Nullable
  @Override
  public E getValue() {
    return myModel.get(mySelectedIndex);
  }

  @RequiredUIAccess
  @Override
  public void setValue(E value, boolean fireListeners) {
    setValueByIndex(myModel.indexOf(value));
  }
}
