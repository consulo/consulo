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
package consulo.web.internal.ui;

import com.vaadin.flow.component.button.ButtonVariant;
import consulo.localize.LocalizeValue;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ToggleButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.base.WebInputDetails;
import consulo.web.internal.ui.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

/**
 * Vaadin has no toggle button, so the pressed state is kept here and shown by the {@code pressed} attribute the
 * theme styles - the same way the aria state is expressed on the element.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebToggleButtonImpl extends VaadinComponentDelegate<WebToggleButtonImpl.Vaadin> implements ToggleButton {
    public class Vaadin extends com.vaadin.flow.component.button.Button implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebToggleButtonImpl.this;
        }
    }

    private LocalizeValue myTextValue;

    private @Nullable Image myImage;

    private boolean mySelected;

    public WebToggleButtonImpl(LocalizeValue text) {
        Vaadin component = toVaadinComponent();

        WebInputDetails.addClickListener(component.getElement(), this::invoke);

        myTextValue = text;
        component.setText(text.get());

        updateSelectedState();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Boolean getValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable Boolean value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (mySelected == value) {
            return;
        }

        mySelected = value;
        updateSelectedState();

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, value));
        }
    }

    private void updateSelectedState() {
        toVaadinComponent().getElement().setAttribute("aria-pressed", String.valueOf(mySelected));
        toVaadinComponent().getElement().getThemeList().set("pressed", mySelected);
    }

    /**
     * A click is what flips a toggle - the value listeners see the new state, the click listeners still run so
     * an action bound to the button performs with it.
     */
    @RequiredUIAccess
    @Override
    public void invoke(InputDetails inputDetails) {
        setValue(!mySelected);

        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, inputDetails));
    }

    @Override
    public void addStyle(ButtonStyle style) {
        switch (style) {
            case PRIMARY:
                toVaadinComponent().addThemeVariants(ButtonVariant.PRIMARY);
                break;
            case BORDERLESS:
                toVaadinComponent().addThemeVariants(ButtonVariant.TERTIARY);
                break;
            case INPLACE:
                toVaadinComponent().addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                break;
        }
    }

    @Override
    public LocalizeValue getText() {
        return myTextValue;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myTextValue = text;
        toVaadinComponent().setText(text.get());
    }

    @Override
    public @Nullable Image getIcon() {
        return myImage;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image image) {
        myImage = image;

        toVaadinComponent().setIcon(image == null ? null : WebImageConverter.getImage(image));
    }
}
