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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebHyperlinkImpl extends VaadinComponentDelegate<WebHyperlinkImpl.Vaadin> implements Hyperlink {
    @Tag("a")
    public class Vaadin extends SimpleComponent implements ClickNotifier<Vaadin>, HasText, FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebHyperlinkImpl.this;
        }
    }

    public WebHyperlinkImpl() {
        toVaadinComponent().addClickListener(event -> {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, ""));
        });
    }

    
    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    
    @Override
    public LocalizeValue getText() {
        return LocalizeValue.of(getVaadinComponent().getText());
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        getVaadinComponent().setText(text.get());
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        // TODO getVaadinComponent().setImage(icon);
    }

    @Override
    public @Nullable Image getIcon() {
        return null;
        // TODO return getVaadinComponent().myImage;
    }
}
