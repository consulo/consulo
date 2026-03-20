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
package consulo.web.internal.ui;

import com.vaadin.flow.component.button.ButtonVariant;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebButtonImpl extends VaadinComponentDelegate<WebButtonImpl.Vaadin> implements Button {

    public class Vaadin extends com.vaadin.flow.component.button.Button implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebButtonImpl.this;
        }
    }

    private LocalizeValue myTextValue = LocalizeValue.empty();

    public WebButtonImpl(LocalizeValue text) {
        Vaadin component = toVaadinComponent();

        component.addClickListener(event -> {
            getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this));
        });

        myTextValue = text;
        component.setText(text.get());
    }

    @Override
    public void addStyle(ButtonStyle style) {
        switch (style) {
            case PRIMARY:
                toVaadinComponent().addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                break;
            case BORDERLESS:
                break;
            case INPLACE:
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
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image image) {
        // TODO  toVaadinComponent().setIcon(image);
    }

    @Override
    public void invoke(InputDetails inputDetails) {

    }

    @Override
    public @Nullable Image getIcon() {
        // TODO
        return null;
        //return toVaadinComponent().myImage;
    }
}
