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

import com.vaadin.flow.component.html.Span;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings("deprecation")
public class WebTextBoxWithExtensionsImpl extends WebTextBoxImpl implements TextBoxWithExtensions {
    private final List<Extension> myExtensions = new ArrayList<>();

    @RequiredUIAccess
    public WebTextBoxWithExtensionsImpl(@Nullable String text) {
        super(text == null ? "" : text);
    }

    /**
     * A text field has one slot per side, so the extensions of a side are rebuilt into a single span together -
     * adding one before another already placed there would otherwise drop it.
     */
    private void updateExtensions() {
        Vaadin field = toVaadinComponent();

        field.setPrefixComponent(buildSide(true));
        field.setSuffixComponent(buildSide(false));
    }

    private com.vaadin.flow.component.@Nullable Component buildSide(boolean left) {
        Span holder = null;

        for (Extension extension : myExtensions) {
            if (extension.isLeft() != left) {
                continue;
            }

            if (holder == null) {
                holder = new Span();
            }

            com.vaadin.flow.component.Component icon = WebImageConverter.getImage(extension.getIcon());

            ComponentEventListener<consulo.ui.Component, ClickEvent> clickListener = extension.getClickListener();
            if (clickListener != null) {
                icon.getStyle().set("cursor", "pointer");

                WebInputDetails.addClickListener(
                    icon.getElement(),
                    inputDetails -> clickListener.onEvent(new ClickEvent(this, inputDetails))
                );
            }

            holder.add(icon);
        }

        return holder;
    }

    @Override
    public TextBoxWithExtensions setExtensions(Extension... extensions) {
        myExtensions.clear();
        myExtensions.addAll(List.of(extensions));

        updateExtensions();
        return this;
    }

    @Override
    public TextBoxWithExtensions addFirstExtension(Extension extension) {
        myExtensions.add(0, extension);

        updateExtensions();
        return this;
    }

    @Override
    public TextBoxWithExtensions addLastExtension(Extension extension) {
        myExtensions.add(extension);

        updateExtensions();
        return this;
    }
}
