/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl.textBox;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.validableComponent.SwingValidableComponent;
import consulo.ui.Component;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.TextFieldWithHistory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-24
 */
public class DesktopTextBoxWithHistoryImpl extends SwingValidableComponent<String, DesktopTextBoxWithHistoryImpl.MyTextFieldWithHistory> implements TextBoxWithHistory {
    public class MyTextFieldWithHistory extends TextFieldWithHistory implements FromSwingComponentWrapper {

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopTextBoxWithHistoryImpl.this;
        }
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {

    }

    @Nullable
    @Override
    public Component getSuffixComponent() {
        return null;
    }

    public DesktopTextBoxWithHistoryImpl(String text) {
        MyTextFieldWithHistory component = new MyTextFieldWithHistory();
        component.setHistorySize(-1);
        component.setText(text);
        initialize(component);
    }

    @Nonnull
    @Override
    public TextBoxWithHistory setHistory(@Nonnull List<String> history) {
        toAWTComponent().setHistory(history);
        return this;
    }

    @Override
    public void selectAll() {
        toAWTComponent().selectText();
    }

    @Override
    public void setEditable(boolean editable) {
        toAWTComponent().setEditable(editable);
    }

    @Override
    public boolean isEditable() {
        return toAWTComponent().isEditable();
    }

    @Override
    public String getValue() {
        return toAWTComponent().getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        toAWTComponent().setText(value);
    }
}
