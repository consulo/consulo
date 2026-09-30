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

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.font.Font;
import consulo.util.lang.StringUtil;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class WebTextAreaImpl extends VaadinComponentDelegate<WebTextAreaImpl.Vaadin> implements consulo.ui.TextArea {
    public class Vaadin extends TextArea implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebTextAreaImpl.this;
        }
    }

    private boolean myFireListeners = true;

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public WebTextAreaImpl(String text) {
        Vaadin area = getVaadinComponent();
        area.setValue(StringUtil.notNullize(text));
        area.setValueChangeMode(ValueChangeMode.LAZY);
        area.addValueChangeListener(event -> {
            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, event.getValue()));
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public String getValue() {
        return getVaadinComponent().getValue();
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable String value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            getVaadinComponent().setValue(StringUtil.notNullize(value));
        }
        finally {
            myFireListeners = true;
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        getVaadinComponent().setPlaceholder(text.isEmpty() ? null : text.get());
    }

    @Override
    @RequiredUIAccess
    public void setEditable(boolean editable) {
        getVaadinComponent().setReadOnly(!editable);
    }

    @Override
    public boolean isEditable() {
        return !getVaadinComponent().isReadOnly();
    }

    @Override
    @RequiredUIAccess
    public void selectAll() {
        getVaadinComponent().getElement().executeJs("this.inputElement && this.inputElement.select()");
    }

    @Override
    @RequiredUIAccess
    public void setFont(@Nullable Font font) {
        Style style = getVaadinComponent().getStyle();
        if (font == null) {
            style.remove("font-family");
            style.remove("--vaadin-input-field-value-font-size");
            style.remove("--vaadin-input-field-value-line-height");
            return;
        }

        style.set("font-family", "\"" + font.getFamily() + "\"");
        style.set("--vaadin-input-field-value-font-size", font.getFontSize() + "px");
        style.set("--vaadin-input-field-value-line-height", "normal");
    }
}
