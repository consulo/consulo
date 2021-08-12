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

import consulo.ui.ListBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.ListModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtListBoxImpl<E> extends SWTComponentDelegate<List> implements ListBox<E> {

  private final ListModel<E> myModel;

  public DesktopSwtListBoxImpl(ListModel<E> model) {
    myModel = model;
  }

  @Override
  protected List createSWT(Composite parent) {
    return new List(parent, SWT.NONE);
  }

  @Override
  protected void initialize(List component) {
    component.setItems("1", "2");
  }

  @Nonnull
  @Override
  public ListModel<E> getListModel() {
    return myModel;
  }

  @Override
  public void setRender(@Nonnull TextItemRender<E> render) {

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
  public void setValue(E value, boolean fireListeners) {

  }
}
