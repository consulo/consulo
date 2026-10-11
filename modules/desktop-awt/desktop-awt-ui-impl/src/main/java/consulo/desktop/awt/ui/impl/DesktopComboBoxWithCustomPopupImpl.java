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

import consulo.desktop.awt.ui.impl.action.CustomPopupComboBox;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.model.FlatDataModel;

import java.awt.Dimension;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopComboBoxWithCustomPopupImpl<E> extends DesktopSingleComboBoxBaseImpl<E, DesktopComboBoxWithCustomPopupImpl<E>.ShellComboBox>
    implements ComboBoxWithCustomPopup<E> {
    class ShellComboBox extends CustomPopupComboBox<E> implements FromSwingComponentWrapper {
        @Override
        public Component toUIComponent() {
            return DesktopComboBoxWithCustomPopupImpl.this;
        }

        @Override
        public Dimension getPreferredSize() {
            return withSuffixWidth(this, super.getPreferredSize());
        }

        @Override
        public void showPopupImpl() {
            getListenerDispatcher(ClickEvent.class)
                .onEvent(new ClickEvent(DesktopComboBoxWithCustomPopupImpl.this, DesktopAWTInputDetails.currentEvent(this)));
        }

        @Override
        public void hidePopupImpl() {
        }

        @Override
        public boolean isCustomPopupVisible() {
            return false;
        }
    }

    public DesktopComboBoxWithCustomPopupImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    public Disposable addClickListener(ComponentEventListener<Component, ClickEvent> clickListener) {
        return addListener(ClickEvent.class, clickListener);
    }

    @Override
    protected ShellComboBox createComponent() {
        ShellComboBox component = new ShellComboBox();
        component.setModel(new DesktopFlatDataModelWrapper<>(myModel));
        initComboBox(component);
        return component;
    }
}
