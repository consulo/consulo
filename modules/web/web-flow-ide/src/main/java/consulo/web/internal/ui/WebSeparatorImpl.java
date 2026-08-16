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
import com.vaadin.flow.dom.Style;
import consulo.ui.Component;
import consulo.ui.Separator;
import consulo.ui.SeparatorStyle;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.AuraUtility;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class WebSeparatorImpl extends VaadinComponentDelegate<WebSeparatorImpl.Vaadin> implements Separator {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebSeparatorImpl.this;
        }
    }

    private static final int LINE_LENGTH = 16;
    // the row a toolbar separator stands in already spaces its children out, so the margin only adds what the
    // divider needs on top of that gap
    private static final int LINE_MARGIN = 2;

    private final SeparatorStyle myStyle;

    public WebSeparatorImpl(SeparatorStyle style) {
        myStyle = style;

        toVaadinComponent().addClassName(AuraUtility.Background.CONTRAST_20);

        Style vaadinStyle = toVaadinComponent().getStyle();
        vaadinStyle.set("flex", "0 0 auto");
        // a rule which stretched over the cross axis of the row would be as tall as the whole button box, while a
        // toolbar divider is only as long as the icons standing beside it
        vaadinStyle.set("align-self", "center");

        if (style == SeparatorStyle.VERTICAL) {
            vaadinStyle.set("width", "1px");
            vaadinStyle.set("height", LINE_LENGTH + "px");
            vaadinStyle.set("margin", "0 " + LINE_MARGIN + "px");
        }
        else {
            vaadinStyle.set("height", "1px");
            vaadinStyle.set("width", LINE_LENGTH + "px");
            vaadinStyle.set("margin", LINE_MARGIN + "px 0");
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public SeparatorStyle getSeparatorStyle() {
        return myStyle;
    }
}
