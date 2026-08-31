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

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTextBoxWithHistoryImpl extends QtComponentDelegate<QLineEdit> implements TextBoxWithHistory {
    private final List<Validator<String>> myValidators = new ArrayList<>();

    private final String myText;

    public DesktopQtTextBoxWithHistoryImpl(String text) {
        myText = StringUtil.notNullize(text);
    }

    @Override
    protected QLineEdit createQt(QWidget parent) {
        return new QLineEdit(parent);
    }

    @Override
    protected void initialize(QLineEdit component) {
        super.initialize(component);

        component.setText(myText);
    }

    @Override
    public TextBoxWithHistory setHistory(List<String> history) {
        return this;
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void setEditable(boolean editable) {
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        myValidators.add(validator);

        return () -> myValidators.remove(validator);
    }

    @RequiredUIAccess
    @Override
    public boolean validate() {
        for (Validator<String> validator : myValidators) {
            if (validator.validateValue(getValue()) != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable String getValue() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return null;
    }

    @Override
    public void setPlaceholder(consulo.localize.LocalizeValue text) {
    }
}
