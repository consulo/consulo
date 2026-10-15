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
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Length;
import consulo.ui.TextItemRenderer;
import consulo.ui.ex.awt.ComboboxSpeedSearch;
import consulo.ui.ex.awt.internal.AWTComboBoxStyle;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemEvent;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
abstract class DesktopComboBoxBaseImpl<E, C extends JComponent> extends SwingComponentDelegate<C> implements ComboBoxBase<E> {
    protected final FlatDataModel<E> myModel;

    protected TextItemRenderer<E> myTextRenderer = TextItemRenderer.defaultRenderer();
    protected @Nullable ComponentItemRenderer<E> myComponentRenderer;
    private @Nullable Function<E, String> mySpeedSearchConverter;
    private @Nullable Function<E, Length> myItemHeightGetter;

    protected DesktopComboBoxBaseImpl(FlatDataModel<E> model) {
        myModel = model;
    }

    protected abstract JComboBox<E> getComboBox(C component);

    protected void initComboBox(JComboBox<E> comboBox) {
        applyRenderer(comboBox);
        applySpeedSearch(comboBox);
    }

    protected void onRendererChanged(C component) {
    }

    private void applyRenderer(JComboBox<E> comboBox) {
        ListCellRenderer<E> renderer = myComponentRenderer != null
            ? new DesktopComponentItemRenderingAdapter<>(myComponentRenderer, () -> -1)
            : new DesktopListRenderer<>(() -> myTextRenderer);

        comboBox.setRenderer(DesktopItemHeightRenderer.wrap(renderer, () -> myItemHeightGetter));
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
                AWTComboBoxStyle.makeTransparent(comboBox);
                break;
            case INPLACE:
                AWTComboBoxStyle.makeBorderInline(comboBox);
                fitToSelection(comboBox);
                comboBox.addItemListener(event -> {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        fitToSelection(comboBox);
                    }
                });
                break;
            case TOOLBAR:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static <E> void fitToSelection(JComboBox<E> comboBox) {
        comboBox.setPrototypeDisplayValue((E) comboBox.getSelectedItem());
        comboBox.revalidate();
    }

    @Override
    public FlatDataModel<E> getDataModel() {
        return myModel;
    }

    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        myTextRenderer = renderer;
        myComponentRenderer = null;
        refreshRenderer();
    }

    @Override
    public void setRenderer(ComponentItemRenderer<E> renderer) {
        myComponentRenderer = renderer;
        refreshRenderer();
    }

    private void refreshRenderer() {
        if (isInitialized()) {
            C component = toAWTComponent();
            applyRenderer(getComboBox(component));
            onRendererChanged(component);
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
