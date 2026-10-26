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

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import consulo.ui.Component;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRenderer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BreadcrumbClickEvent;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The vaadin breadcrumbs component, which also collapses the middle items into an overflow menu when they do not fit.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public class WebBreadcrumbsImpl<E> extends VaadinComponentDelegate<WebBreadcrumbsImpl.Vaadin> implements consulo.ui.Breadcrumbs<E> {
    public class Vaadin extends Breadcrumbs implements FromVaadinComponentWrapper {
        public Vaadin() {
            // the default ROUTER mode rebuilds the crumbs from the vaadin route on every navigation, wiping ours
            super(Mode.MANUAL);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebBreadcrumbsImpl.this;
        }
    }

    private final List<E> myItems = new ArrayList<>();
    private TextItemRenderer<E> myRenderer = TextItemRenderer.defaultRenderer();

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<? extends E> items) {
        myItems.clear();
        myItems.addAll(items);
        rebuild();
    }

    @Override
    public List<E> getItems() {
        return List.copyOf(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        myRenderer = renderer;
        rebuild();
    }

    private void rebuild() {
        Vaadin vaadin = toVaadinComponent();
        vaadin.removeAll();

        for (E item : myItems) {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            myRenderer.render(presentation, RenderItem.of(item, false));

            StringBuilder text = new StringBuilder();
            for (WebItemPresentationImpl.Fragment fragment : presentation.getFragments()) {
                text.append(fragment.text());
            }

            BreadcrumbsItem crumb = new BreadcrumbsItem(text.toString());
            Image icon = presentation.getIcon();
            if (icon != null) {
                crumb.setPrefixComponent(WebImageConverter.getImage(icon));
            }
            String tooltip = presentation.getTooltip().get();
            if (!tooltip.isEmpty()) {
                crumb.getElement().setAttribute("title", tooltip);
            }

            // BreadcrumbsItem only navigates by route, so the click is wired by hand
            WebInputDetails.addClickListener(crumb.getElement(), details ->
                getListenerDispatcher(BreadcrumbClickEvent.class).onEvent(new BreadcrumbClickEvent<>(this, item, details)));

            vaadin.add(crumb);
        }
    }
}
