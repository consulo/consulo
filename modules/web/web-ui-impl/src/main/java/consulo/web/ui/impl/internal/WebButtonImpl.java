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

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.dom.Style;
import consulo.localize.LocalizeValue;
import consulo.ui.util.TextWithMnemonic;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebButtonImpl extends VaadinComponentDelegate<WebButtonImpl.Vaadin> implements Button {
    @StyleSheet("/button/webButton.css")
    public class Vaadin extends com.vaadin.flow.component.button.Button implements FromVaadinComponentWrapper {
        public Vaadin() {
            // the label of a button is one line, as it is of a swing button - a narrow column broke "Copy to
            // clipboard" over two lines and made the button twice as tall as the one beside it
            getStyle().set("white-space", "nowrap");
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebButtonImpl.this;
        }
    }

    private LocalizeValue myTextValue = LocalizeValue.empty();

    private @Nullable Image myImage;

    public WebButtonImpl(LocalizeValue text) {
        Vaadin component = toVaadinComponent();

        WebInputDetails.addClickListener(component.getElement(), this::invoke);
        myClickInstalled = true;

        myTextValue = text;
        component.setText(plainText(text));
    }

    /**
     * The browser draws no mnemonic, so a text which carries one has to be stripped of the marker rather than
     * show it - a close button of a dialog reads "&Close" otherwise.
     */
    private static String plainText(LocalizeValue text) {
        return TextWithMnemonic.parse(text.get()).getText();
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
                applyInplaceStyle(toVaadinComponent());
                break;
            case TOOLBAR:
                applyToolbarStyle(toVaadinComponent());
                break;
        }
    }

    /**
     * The geometry lives in {@code webButton.css} rather than in inline properties, because a toolbar button is
     * square only while it carries no label - and whether it will is not known when the style is added.
     */
    static void applyToolbarStyle(com.vaadin.flow.component.button.Button button) {
        button.addThemeVariants(ButtonVariant.TERTIARY);
        button.addClassName("consulo-toolbar-button");
    }

    static void applyInplaceStyle(com.vaadin.flow.component.button.Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.addClassName("consulo-inplace-button");

        Style style = button.getStyle();
        style.set("--vaadin-button-height", "auto");
        style.set("--vaadin-button-margin", "0");
        style.set("--vaadin-button-padding", "0");
        style.set("--vaadin-button-border-width", "0");
        style.set("--vaadin-button-background", "transparent");
    }

    @Override
    public LocalizeValue getText() {
        return myTextValue;
    }

    @Override
    @RequiredUIAccess
    public void setText(LocalizeValue text) {
        myTextValue = text;
        toVaadinComponent().setText(plainText(text));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public void setIcon(@Nullable Image image) {
        myImage = image;

        toVaadinComponent().setIcon(image == null ? null : WebImageConverter.getImage(image));
    }

    @Override
    public void invoke(InputDetails inputDetails) {
        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, inputDetails));
    }

    @Override
    public @Nullable Image getIcon() {
        return myImage;
    }
}
