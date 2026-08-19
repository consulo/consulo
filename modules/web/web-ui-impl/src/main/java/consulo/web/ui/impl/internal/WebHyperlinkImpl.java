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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import consulo.web.ui.impl.internal.vaadin.SimpleComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebHyperlinkImpl extends VaadinComponentDelegate<WebHyperlinkImpl.Vaadin> implements Hyperlink {
    @Tag("a")
    @StyleSheet("/hyperlink/webHyperlink.css")
    public class Vaadin extends SimpleComponent implements ClickNotifier<Vaadin>, FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebHyperlinkImpl.this;
        }
    }

    private final Span myTextLabel = new Span();

    private LocalizeValue myText = LocalizeValue.empty();
    private @Nullable Image myIcon;
    private com.vaadin.flow.component.@Nullable Component myIconComponent;

    public WebHyperlinkImpl() {
        Vaadin vaadin = getVaadinComponent();
        vaadin.addClassName("consulo-hyperlink");
        vaadin.getStyle().set("color", WebColors.toCssColor(ComponentColors.LINK_FOREGROUND));

        vaadin.getElement().appendChild(myTextLabel.getElement());

        vaadin.addClickListener(
            event -> getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, "", WebInputDetails.details(event)))
        );
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    @RequiredUIAccess
    public void setText(LocalizeValue text) {
        myText = text;
        myTextLabel.setText(text.get());
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;

        com.vaadin.flow.component.@Nullable Component oldIconComponent = myIconComponent;
        if (oldIconComponent != null) {
            oldIconComponent.getElement().removeFromParent();
            myIconComponent = null;
        }

        if (icon != null) {
            com.vaadin.flow.component.Component iconComponent = WebImageConverter.getImage(icon);
            myIconComponent = iconComponent;

            getVaadinComponent().getElement().insertChild(0, iconComponent.getElement());
        }
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }
}
