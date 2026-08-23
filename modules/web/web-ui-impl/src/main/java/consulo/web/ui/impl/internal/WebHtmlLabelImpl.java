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

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.vaadin.VaadinLabelComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebHtmlLabelImpl extends WebLabelBase<WebHtmlLabelImpl.Vaadin> implements HtmlLabel {
    public class Vaadin extends VaadinLabelComponentBase {
        @Override
        public void setText(LocalizeValue text) {
            super.setText(text);

            // the markup is the content, so the text node the base built is dropped in favour of it
            getElement().removeAllChildren();
            getElement().setProperty("innerHTML", text.get());
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebHtmlLabelImpl.this;
        }
    }

    @RequiredUIAccess
    public WebHtmlLabelImpl(LocalizeValue html, LabelOptions options) {
        super(html, options);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
