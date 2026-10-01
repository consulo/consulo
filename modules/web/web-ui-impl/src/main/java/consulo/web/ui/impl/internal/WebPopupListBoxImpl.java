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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.Element;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.internal.InlineButton;
import consulo.ui.ex.internal.InlineButtonsList;
import consulo.ui.model.FlatDataModel;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
class WebPopupListBoxImpl<E> extends WebListBoxImpl<E> implements InlineButtonsList<E> {
    private static final String ROW_CLASS = "web-list-box-inline-row";
    private static final String CONTENT_CLASS = "web-list-box-inline-content";
    private static final String BUTTONS_CLASS = "web-list-box-inline-buttons";
    private static final String BUTTON_CLASS = "web-list-box-inline-button";

    private static final String ALWAYS_ATTRIBUTE = "always";
    private static final String ACTIVE_ATTRIBUTE = "active";

    private @Nullable Function<E, List<InlineButton>> myInlineButtons;

    private final Map<E, List<Element>> myButtonElements = new HashMap<>();

    private @Nullable Element myActiveButton;

    WebPopupListBoxImpl(FlatDataModel<E> model) {
        super(model);

        WebInputDetails.addKeyListener(toVaadinComponent().getElement(), "keydown", details -> {
            if (getValue() != null) {
                getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details));
            }
        }).setFilter("event.key === 'Enter'");

        model.addListener(event -> toVaadinComponent().getElement().executeJs(
            """
            const list = this;
            setTimeout(() => {
                if (document.activeElement && document.activeElement !== document.body) {
                    return;
                }

                const item = list.selected != null ? list.children[list.selected] : null;
                if (item && typeof item.focus === 'function') {
                    item.focus({ preventScroll: true });
                }
                else {
                    list.focus();
                }
            });
            """
        ));
    }

    @Override
    @RequiredUIAccess
    public void setInlineButtons(Function<E, List<InlineButton>> buttons) {
        myInlineButtons = buttons;

        applyRender();
    }

    @Override
    @RequiredUIAccess
    public void setActiveInlineButton(int index) {
        Element previous = myActiveButton;
        if (previous != null) {
            previous.removeAttribute(ACTIVE_ATTRIBUTE);
            myActiveButton = null;
        }

        E value = getValue();
        List<Element> elements = value == null ? null : myButtonElements.get(value);
        if (elements == null || index < 0 || index >= elements.size()) {
            return;
        }

        Element active = elements.get(index);
        active.setAttribute(ACTIVE_ATTRIBUTE, true);
        myActiveButton = active;
    }

    @Override
    protected Component decorateRow(Component row, @Nullable E item) {
        Function<E, List<InlineButton>> provider = myInlineButtons;
        if (provider == null || item == null) {
            return row;
        }

        List<InlineButton> buttons = provider.apply(item);
        if (buttons.isEmpty()) {
            myButtonElements.remove(item);
            return row;
        }

        row.getElement().getClassList().add(CONTENT_CLASS);

        Div strip = new Div();
        strip.addClassName(BUTTONS_CLASS);

        List<Element> elements = new ArrayList<>(buttons.size());
        for (InlineButton button : buttons) {
            Div element = new Div(WebImageConverter.getImage(button.icon()));
            element.addClassName(BUTTON_CLASS);

            if (button.alwaysVisible()) {
                element.getElement().setAttribute(ALWAYS_ATTRIBUTE, true);
            }

            if (button.toolTip().isNotEmpty()) {
                Tooltip.forComponent(element).setText(button.toolTip().get());
            }

            WebInputDetails.addClickListener(element.getElement(), button.action()).stopPropagation();

            strip.add(element);
            elements.add(element.getElement());
        }

        myButtonElements.put(item, elements);

        Div container = new Div(row, strip);
        container.addClassName(ROW_CLASS);
        return container;
    }
}
