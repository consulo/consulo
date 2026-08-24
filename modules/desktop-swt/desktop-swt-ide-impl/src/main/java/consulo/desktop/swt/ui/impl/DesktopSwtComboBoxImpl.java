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
import consulo.ui.ComboBox;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ComponentItemRender;
import java.util.function.Function;
import consulo.ui.model.FlatDataModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2021-04-29
 */
public class DesktopSwtComboBoxImpl<E> extends SWTComponentDelegate<CCombo> implements ComboBox<E> {
    private TextItemRender<E> myRenderer = (renderer, item) -> {
        if (item == null) {
            renderer.append("");
        }
        else {
            renderer.append(item.toString());
        }
    };

    private final FlatDataModel<E> myModel;

    private int mySelectedIndex = 0;

    public DesktopSwtComboBoxImpl(FlatDataModel<E> model) {
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

            myRenderer.render(presentation, RenderItem.of(element, i == mySelectedIndex));

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

    
    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myRenderer = render;
    }

    @Override
    public void setValueByIndex(int index) {
        mySelectedIndex = index;

        if (myComponent != null) {
            myComponent.select(index);
        }
    }

    @Override
    public @Nullable E getValue() {
        return myModel.get(mySelectedIndex);
    }

    @Override
    @RequiredUIAccess
    public void setValue(E value, boolean fireListeners) {
        setValueByIndex(myModel.indexOf(value));
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
}
