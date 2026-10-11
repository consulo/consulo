/*
 * Copyright 2013-2016 consulo.io
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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.model.FlatDataModel;

import java.awt.Dimension;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopComboBoxImpl<E> extends DesktopSingleComboBoxBaseImpl<E, DesktopComboBoxImpl<E>.MyComboBox> {
    class MyComboBox extends consulo.ui.ex.awt.ComboBox<E> implements FromSwingComponentWrapper {
        @Override
        public Component toUIComponent() {
            return DesktopComboBoxImpl.this;
        }

        @Override
        public Dimension getPreferredSize() {
            return withSuffixWidth(this, super.getPreferredSize());
        }
    }

    public DesktopComboBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected MyComboBox createComponent() {
        MyComboBox component = new MyComboBox();
        component.setModel(new DesktopFlatDataModelWrapper<>(myModel));
        initComboBox(component);
        return component;
    }
}
