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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.ui.ComboBoxStyle;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRender;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.model.FlatDataModel;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.vaadin.WebSingleListComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-22
 */
@SuppressWarnings("unchecked")
public class WebComboBoxWithCustomPopupImpl<V> extends WebSingleListComponentBase<V, WebComboBoxWithCustomPopupImpl.Vaadin>
    implements ComboBoxWithCustomPopup<V> {

    public class Vaadin extends Select<V> implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebComboBoxWithCustomPopupImpl.this;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            super.onAttach(attachEvent);

            getElement().executeJs("""
                const el = this;
                if (el.__consuloComboShell) {
                    return;
                }
                el.__consuloComboShell = true;

                const block = event => {
                    event.stopImmediatePropagation();
                    event.preventDefault();
                };
                el.addEventListener('mousedown', block, true);
                el.addEventListener('click', event => {
                    block(event);
                    el.dispatchEvent(new MouseEvent($0, {
                        clientX: event.clientX,
                        clientY: event.clientY,
                        screenX: event.screenX,
                        screenY: event.screenY,
                        button: event.button,
                        altKey: event.altKey,
                        ctrlKey: event.ctrlKey,
                        shiftKey: event.shiftKey,
                        metaKey: event.metaKey
                    }));
                }, true);
                """, SHELL_PRESS_EVENT);
        }
    }

    private static final String SHELL_PRESS_EVENT = "consulo-combo-shell-press";

    public WebComboBoxWithCustomPopupImpl(FlatDataModel<V> model) {
        super(model);

        setRender(TextItemRender.defaultRender());

        WebInputDetails.addClickListener(
            toVaadinComponent().getElement(),
            SHELL_PRESS_EVENT,
            details -> {
                // the press is killed in the capture phase, so the tracker of activity never sees it unless
                // the control which swallowed it says so itself
                ActivityTracker.getInstance().inc();

                getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details));
            }
        );

        myClickInstalled = true;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void addStyle(ComboBoxStyle style) {
        switch (style) {
            case TRANSPARENT_BACKGROUND:
                toVaadinComponent().getStyle().set("--vaadin-input-field-background", "transparent");
                break;
            case INPLACE:
                toVaadinComponent().getStyle()
                    .set("--vaadin-input-field-border-width", "0")
                    .set("--vaadin-input-field-border-radius", "0")
                    .set("--vaadin-focus-ring-width", "0");
                break;
        }
    }

    @Override
    public void setRender(TextItemRender<V> render) {
        myTextRender = render;

        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            render.render(presentation, RenderItem.of((V) item, isSelected((V) item)));

            com.vaadin.flow.component.Component component = presentation.toComponent();
            applyItemHeight(component, (V) item);
            return component;
        }));
    }

    @Override
    public void setRender(ComponentItemRender<V> render) {
        toVaadinComponent().setRenderer(new ComponentRenderer<>(item -> {
            Component rendered = render.render(RenderItem.of((V) item, isSelected((V) item)));

            com.vaadin.flow.component.Component component = ((ToVaadinComponentWrapper) rendered).toVaadinComponent();
            applyItemHeight(component, (V) item);
            return component;
        }));
    }

    private boolean isSelected(@Nullable V item) {
        return item != null && item.equals(getValue());
    }
}
