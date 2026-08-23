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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.checkbox.Checkbox;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBoxStyle;
import consulo.ui.Component;
import consulo.ui.TriStateCheckBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.lang.ThreeState;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebTriStateCheckBoxImpl extends VaadinComponentDelegate<WebTriStateCheckBoxImpl.Vaadin> implements TriStateCheckBox {
    public class Vaadin extends Checkbox implements FromVaadinComponentWrapper {
        private LocalizeValue myLabelText = LocalizeValue.empty();

        public Vaadin() {
            getStyle().set("white-space", "nowrap");
        }

        public void setLabelText(LocalizeValue labelText) {
            myLabelText = labelText;
            updateLabelText();
        }

        public LocalizeValue getLabelText() {
            return myLabelText;
        }

        private void updateLabelText() {
            TextWithMnemonic textWithMnemonic = TextWithMnemonic.parse(myLabelText.get());

            setLabel(textWithMnemonic.getText());
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebTriStateCheckBoxImpl.this;
        }
    }

    private ThreeState myValue = ThreeState.UNSURE;

    private boolean myUnsureEnabled = true;

    public WebTriStateCheckBoxImpl() {
        Vaadin component = getVaadinComponent();
        component.setIndeterminate(true);
        component.addValueChangeListener(event -> {
            if (!event.isFromClient()) {
                return;
            }

            component.setIndeterminate(false);
            myValue = event.getValue() ? ThreeState.YES : ThreeState.NO;
            fireValueEvent();
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public ThreeState getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable ThreeState value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        myValue = value == null ? ThreeState.UNSURE : value;

        Vaadin component = getVaadinComponent();
        component.setValue(myValue == ThreeState.YES);
        component.setIndeterminate(myValue == ThreeState.UNSURE);

        if (fireListeners) {
            fireValueEvent();
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void fireValueEvent() {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, myValue));
    }

    @Override
    public boolean isUnsureEnabled() {
        return myUnsureEnabled;
    }

    @RequiredUIAccess
    @Override
    public void setUnsureEnabled(boolean unsureEnabled) {
        myUnsureEnabled = unsureEnabled;
    }

    @Override
    public void addStyle(CheckBoxStyle style) {
    }

    @Override
    public LocalizeValue getLabelText() {
        return toVaadinComponent().getLabelText();
    }

    @Override
    @RequiredUIAccess
    public void setLabelText(LocalizeValue textValue) {
        UIAccess.assertIsUIThread();

        toVaadinComponent().setLabelText(textValue);
    }
}
