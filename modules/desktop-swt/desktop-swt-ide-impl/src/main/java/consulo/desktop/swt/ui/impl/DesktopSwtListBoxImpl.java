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

import consulo.ui.Length;
import consulo.ui.TransferHandler;
import consulo.ui.ListBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ComponentItemRender;
import java.util.function.Function;
import consulo.ui.model.FlatDataModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2021-04-29
 */
public class DesktopSwtListBoxImpl<E> extends SWTComponentDelegate<List> implements ListBox<E> {
  private @Nullable TransferHandler<E> myTransferHandler;
    private final FlatDataModel<E> myModel;

    public DesktopSwtListBoxImpl(FlatDataModel<E> model) {
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

    
    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
    }

    @Override
    public void setValueByIndex(int index) {
    }

    @Override
    public @Nullable E getValue() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
    }

  @Override
  public void setTransferHandler(@Nullable TransferHandler<E> handler) {
      myTransferHandler = handler;
  }

  @Override
  public @Nullable TransferHandler<E> getTransferHandler() {
      return myTransferHandler;
  }

    @Override
    public void setPlaceholder(consulo.localize.LocalizeValue text) {
    }
}
