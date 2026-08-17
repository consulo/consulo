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

import com.vaadin.flow.component.dependency.StyleSheet;
import consulo.logging.Logger;
import consulo.ui.ColorBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;
import org.vaadin.addons.tatu.ColorPicker;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class WebColorBoxImpl extends VaadinComponentDelegate<WebColorBoxImpl.Vaadin> implements ColorBox {
    private static final Logger LOG = Logger.getInstance(WebColorBoxImpl.class);

    @StyleSheet("/colorBox/webColorBox.css")
    public class Vaadin extends ColorPicker implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebColorBoxImpl.this;
        }
    }

    private @Nullable ColorValue myValue;

    @RequiredUIAccess
    public WebColorBoxImpl(@Nullable ColorValue colorValue) {
        Vaadin vaadin = getVaadinComponent();
        // the preset dropdown the addon puts beside the swatch lists nothing, as no preset is given, and is
        // the only way the colour could be emptied - it is a part, so only a rule reaches it
        vaadin.addClassName("consulo-color-box");
        vaadin.setNoClear(true);

        setValue(colorValue, false);

        vaadin.addValueChangeListener(event -> {
            myValue = toColorValue(event.getValue());

            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, myValue));
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable ColorValue getValue() {
        return myValue;
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable ColorValue value, boolean fireListeners) {
        myValue = value;

        String cssColor = WebColors.toCssColor(value);
        getVaadinComponent().setValue(cssColor == null ? "" : cssColor);

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, value));
        }
    }

    /**
     * The component answers the colour of its swatch as css, and only a hex one is a colour the platform can
     * hold - a named or a function one has no {@link RGBColor} to decode into.
     */
    private static @Nullable ColorValue toColorValue(@Nullable String cssColor) {
        if (cssColor == null || cssColor.isEmpty()) {
            return null;
        }

        try {
            return RGBColor.decode(cssColor);
        }
        catch (NumberFormatException e) {
            LOG.warn("Cannot read color: " + cssColor);
            return null;
        }
    }

    @Override
    public void setEditable(boolean editable) {
        getVaadinComponent().setReadOnly(!editable);
    }

    @Override
    public boolean isEditable() {
        return !getVaadinComponent().isReadOnly();
    }
}
