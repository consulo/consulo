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

import com.vaadin.flow.component.html.Div;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebImageBoxImpl extends VaadinComponentDelegate<WebImageBoxImpl.Vaadin> implements ImageBox {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebImageBoxImpl.this;
        }
    }

    private final Image myImage;

    public WebImageBoxImpl(Image image) {
        myImage = image;

        Vaadin component = toVaadinComponent();
        // the image brings its own size, the box is only what holds it
        component.getStyle().set("display", "inline-flex");
        component.add(WebImageConverter.getImage(image));
    }

    @Override
    public Image getImage() {
        return myImage;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
