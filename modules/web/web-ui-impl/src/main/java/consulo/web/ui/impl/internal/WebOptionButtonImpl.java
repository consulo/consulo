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
import consulo.localize.LocalizeValue;
import consulo.ui.MenuItem;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class WebOptionButtonImpl extends WebButtonImpl implements OptionButton {
    private static final String OPTION_BUTTON_CLASS = "consulo-option-button";

    private static final String ARROW_CLASS = "consulo-option-button-arrow";

    private static final String SHOW_OPTIONS_EVENT = "consulo-show-options";

    private List<OptionButtonItem> myOptions = List.of();

    private @Nullable Span myArrow;

    public WebOptionButtonImpl(LocalizeValue text) {
        super(text);

        Vaadin component = toVaadinComponent();
        component.getElement().executeJs(
            """
                this.addEventListener('keydown', e => {
                    if (e.key === 'Enter' && e.altKey && e.shiftKey) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                        this.dispatchEvent(new CustomEvent($0));
                    }
                }, true);
                """,
            SHOW_OPTIONS_EVENT
        );
        component.getElement().addEventListener(SHOW_OPTIONS_EVENT, event -> showOptions());
    }

    @Override
    public List<OptionButtonItem> getOptions() {
        return myOptions;
    }

    @RequiredUIAccess
    @Override
    public void setOptions(List<OptionButtonItem> options) {
        myOptions = List.copyOf(options);

        Vaadin component = toVaadinComponent();
        if (myOptions.isEmpty()) {
            if (myArrow != null) {
                component.setSuffixComponent(null);
                component.removeClassName(OPTION_BUTTON_CLASS);
                myArrow = null;
            }
            return;
        }

        if (myArrow == null) {
            Span arrow = new Span();
            arrow.addClassName(ARROW_CLASS);
            arrow.getElement().addEventListener("click", event -> {
                if (isEnabled()) {
                    showOptions();
                }
            }).addEventData("event.stopPropagation()");

            component.setSuffixComponent(arrow);
            component.addClassName(OPTION_BUTTON_CLASS);
            myArrow = arrow;
        }
    }

    @RequiredUIAccess
    @Override
    public void showOptions() {
        if (myOptions.isEmpty()) {
            return;
        }

        WebPopupMenuImpl menu = new WebPopupMenuImpl(this);
        for (OptionButtonItem option : myOptions) {
            MenuItem item = MenuItem.create(option.text());
            item.setIcon(option.icon());
            item.setEnabled(option.enabled());
            item.addClickListener(event -> option.clickListener().onEvent(new ClickEvent(this, event.getInputDetails())));
            menu.add(item);
        }

        menu.showBelowTarget();
    }
}
