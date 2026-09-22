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
package consulo.desktop.qt.ui.impl;

import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.model.FlatDataModel;
import io.qt.core.Qt;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-09-22
 */
public class DesktopQtComboBoxWithCustomPopupImpl<E> extends DesktopQtComboBoxImpl<E> implements ComboBoxWithCustomPopup<E> {
    private static class QtShellComboBox extends QComboBox {
        QtShellComboBox(QWidget parent) {
            super(parent);
        }

        @Override
        public void showPopup() {
        }

        /**
         * A press is answered by whoever placed the control, so the widget neither drops its list down nor
         * takes the sunken look it would never be brought out of - the release which would do that is the one
         * carrying the press away.
         */
        @Override
        protected void mousePressEvent(QMouseEvent event) {
            if (event.button() == Qt.MouseButton.LeftButton) {
                event.accept();
                return;
            }

            super.mousePressEvent(event);
        }
    }

    public DesktopQtComboBoxWithCustomPopupImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected QComboBox createQt(QWidget parent) {
        return new QtShellComboBox(parent);
    }

    @Override
    protected void initialize(QComboBox component) {
        super.initialize(component);

        component.setSizeAdjustPolicy(QComboBox.SizeAdjustPolicy.AdjustToContents);

        setFocusable(false);
    }
}
