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

import consulo.disposer.Disposable;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.internal.AWTHasSuffixComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.Dimension;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
abstract class DesktopSingleComboBoxBaseImpl<E, C extends JComboBox<E>> extends DesktopComboBoxBaseImpl<E, C> implements ComboBox<E> {
    static Dimension withSuffixWidth(JComboBox<?> comboBox, Dimension size) {
        JComponent suffix = AWTHasSuffixComponent.getSuffixComponent(comboBox);
        if (suffix == null || !suffix.isVisible()) {
            return size;
        }
        return new Dimension(size.width + suffix.getPreferredSize().width, size.height);
    }

    private @Nullable Component mySuffixComponent;

    protected DesktopSingleComboBoxBaseImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        AWTHasSuffixComponent.setSuffixComponent(toAWTComponent(), suffixComponent == null ? null : TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    protected JComboBox<E> getComboBox(C component) {
        return component;
    }

    @Override
    public void setValueByIndex(int index) {
        toAWTComponent().setSelectedIndex(index);
    }

    @RequiredUIAccess
    @Override
    public void setValue(E value, boolean fireListeners) {
        toAWTComponent().setSelectedItem(value);
    }

    @Override
    public Disposable addValueListener(ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> valueListener) {
        DesktopValueListenerAsItemListenerImpl<E> listener = new DesktopValueListenerAsItemListenerImpl<>(this, valueListener, true);
        toAWTComponent().addItemListener(listener);
        return () -> toAWTComponent().removeItemListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable E getValue() {
        return (E) toAWTComponent().getSelectedItem();
    }
}
