/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.ComboBoxBase;
import consulo.ui.ComboBoxStyle;
import consulo.ui.ComponentItemRender;
import consulo.ui.Length;
import consulo.ui.TextItemRender;
import consulo.ui.ex.awt.ComboboxSpeedSearch;
import consulo.ui.ex.awt.internal.AWTComboBoxStyle;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
abstract class DesktopComboBoxBaseImpl<E, C extends JComponent> extends SwingComponentDelegate<C> implements ComboBoxBase<E> {
    protected final FlatDataModel<E> myModel;

    protected TextItemRender<E> myTextRender = TextItemRender.defaultRender();
    protected @Nullable ComponentItemRender<E> myComponentRender;
    private @Nullable Function<E, String> mySpeedSearchConverter;
    private @Nullable Function<E, Length> myItemHeightGetter;

    protected DesktopComboBoxBaseImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    protected abstract JComboBox<E> getComboBox(C component);

    protected void initComboBox(JComboBox<E> comboBox) {
        applyRender(comboBox);
        applySpeedSearch(comboBox);
    }

    protected void onRenderChanged(C component) {
    }

    private void applyRender(JComboBox<E> comboBox) {
        ListCellRenderer<E> render = myComponentRender != null
            ? new DesktopComponentItemRenderAdapter<>(myComponentRender, () -> -1)
            : new DesktopListRender<>(() -> myTextRender);

        comboBox.setRenderer(DesktopItemHeightRender.wrap(render, () -> myItemHeightGetter));
    }

    private void applySpeedSearch(JComboBox<E> comboBox) {
        if (mySpeedSearchConverter != null) {
            ComboboxSpeedSearch.installSpeedSearch(comboBox, mySpeedSearchConverter::apply);
        }
    }

    @Override
    public void addStyle(ComboBoxStyle style) {
        JComboBox<E> comboBox = getComboBox(toAWTComponent());
        switch (style) {
            case TRANSPARENT_BACKGROUND:
                comboBox.setOpaque(false);
                break;
            case INPLACE:
                AWTComboBoxStyle.makeBorderInline(comboBox);
                break;
        }
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRender(TextItemRender<E> render) {
        myTextRender = render;
        myComponentRender = null;
        refreshRender();
    }

    @Override
    public void setRender(ComponentItemRender<E> render) {
        myComponentRender = render;
        refreshRender();
    }

    private void refreshRender() {
        if (isInitialized()) {
            C component = toAWTComponent();
            applyRender(getComboBox(component));
            onRenderChanged(component);
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<E, String> converter) {
        mySpeedSearchConverter = converter;
        if (isInitialized()) {
            applySpeedSearch(getComboBox(toAWTComponent()));
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isInitialized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(getComboBox(toAWTComponent()));
        return supply == null ? null : supply.getEnteredPrefix();
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<E, Length> getter) {
        myItemHeightGetter = getter;
    }
}
