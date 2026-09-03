/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebRadioButtonImpl extends VaadinComponentDelegate<WebRadioButtonImpl.Vaadin> implements RadioButton {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        private final Input myInput;
        private final NativeLabel myLabel;

        public Vaadin() {
            myInput = new Input();
            myInput.setId(UUID.randomUUID().toString());
            myInput.setType("radio");
            myLabel = new NativeLabel();
            myLabel.setFor(myInput);

            getStyle().set("white-space", "nowrap");

            add(myInput, myLabel);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebRadioButtonImpl.this;
        }

        public void setText(LocalizeValue text) {
            myLabel.setText(text.getValue());
        }

        public Input getInput() {
            return myInput;
        }
    }

    private static final String CHECKED = "checked";
    private static final String NAME = "name";

    private LocalizeValue myText = LocalizeValue.empty();

    @RequiredUIAccess
    public WebRadioButtonImpl(boolean selected, LocalizeValue text) {
        setLabelText(text);
        setValue(selected);
        listenChecked();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Boolean getValue() {
        return getVaadinComponent().getInput().getElement().getProperty(CHECKED, false);
    }

    @Override
    @RequiredUIAccess
    public void setValue(Boolean value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        boolean selected = Boolean.TRUE.equals(value);
        if (selected == getValue()) {
            return;
        }

        getVaadinComponent().getInput().getElement().setProperty(CHECKED, selected);

        if (fireListeners) {
            fireValueChanged(selected);
        }
    }

    /**
     * The browser keeps radios of one name exclusive by unchecking the others itself, and says nothing about the
     * ones it unchecked - so every button of the group is asked to report its own property back.
     */
    private void listenChecked() {
        getVaadinComponent().getInput().getElement()
            .addPropertyChangeListener(CHECKED, "change", event -> fireValueChanged(getValue()));
    }

    private void fireValueChanged(boolean selected) {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, selected));
    }

    public void setGroupName(String name) {
        getVaadinComponent().getInput().getElement().setAttribute(NAME, name);
    }

    @Override
    public LocalizeValue getLabelText() {
        return myText;
    }

    @Override
    @RequiredUIAccess
    public void setLabelText(LocalizeValue text) {
        UIAccess.assertIsUIThread();

        myText = text;

        getVaadinComponent().setText(text);
    }
}
