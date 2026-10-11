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

import consulo.localize.LocalizeValue;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.internal.SearchHighlightUtil;
import io.qt.gui.QPaintEvent;
import io.qt.widgets.QRadioButton;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtRadioButtonImpl extends QtComponentDelegate<QRadioButton> implements RadioButton {
    private class HighlightRadioButton extends QRadioButton {
        private HighlightRadioButton(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            String query = mySearchHighlight;
            String text = QtMnemonic.plain(myText);
            if (query == null || !SearchHighlightUtil.matches(text, query)) {
                super.paintEvent(event);
                return;
            }

            QStyleOptionButton option = new QStyleOptionButton();
            initStyleOption(option);

            DesktopQtSearchHighlight.paintCheckable(
                this,
                option,
                QStyle.ControlElement.CE_RadioButton,
                QStyle.SubElement.SE_RadioButtonContents,
                text,
                query
            );
        }
    }

    private LocalizeValue myText;

    private @Nullable String mySearchHighlight;
    private boolean mySelected;

    private boolean myFireListeners = true;

    public DesktopQtRadioButtonImpl(LocalizeValue text, boolean selected) {
        myText = text;
        mySelected = selected;
    }

    @Override
    protected QRadioButton createQt(QWidget parent) {
        return new HighlightRadioButton(parent);
    }

    @Override
    protected void initialize(QRadioButton component) {
        super.initialize(component);

        component.setText(QtMnemonic.withMnemonic(myText));
        component.setChecked(mySelected);

        component.toggled.connect(checked -> {
            mySelected = checked;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, mySelected, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public Boolean getValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    public void setValue(Boolean value, boolean fireListeners) {
        boolean newValue = value != null && value;
        boolean changed = mySelected != newValue;
        mySelected = newValue;

        if (myComponent == null) {
            if (fireListeners && changed) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, mySelected, ProgrammaticInputDetails.INSTANCE));
            }
            return;
        }

        myFireListeners = fireListeners;
        try {
            myComponent.setChecked(mySelected);
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public LocalizeValue getLabelText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setLabelText(LocalizeValue text) {
        myText = text;

        if (myComponent != null) {
            myComponent.setText(QtMnemonic.withMnemonic(text));
        }
    }

    @RequiredUIAccess
    @Override
    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = SearchHighlightUtil.normalizeQuery(query);

        if (myComponent != null) {
            myComponent.update();
        }
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }
}
