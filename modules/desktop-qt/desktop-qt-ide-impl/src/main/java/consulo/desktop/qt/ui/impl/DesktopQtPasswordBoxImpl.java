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
import consulo.ui.PasswordBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.util.lang.StringUtil;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtPasswordBoxImpl extends QtComponentDelegate<QLineEdit> implements PasswordBox {
    private String myText;

    private boolean myFireListeners = true;

    private final List<Validator<String>> myValidators = new ArrayList<>();

    public DesktopQtPasswordBoxImpl(String password) {
        myText = StringUtil.notNullize(password);
    }

    @Override
    protected QLineEdit createQt(QWidget parent) {
        QLineEdit lineEdit = new QLineEdit(parent);
        lineEdit.setEchoMode(QLineEdit.EchoMode.Password);
        return lineEdit;
    }

    @Override
    protected void initialize(QLineEdit component) {
        super.initialize(component);

        component.setText(myText);

        component.textChanged.connect(text -> {
            myText = StringUtil.notNullize(text);

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, myText));
            }
        });
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
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setValue(String value, boolean fireListeners) {
        myText = StringUtil.notNullize(value);

        if (myComponent != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setText(myText);
            }
            finally {
                myFireListeners = true;
            }
        }
    }
}
