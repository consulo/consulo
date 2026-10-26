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
package consulo.it.internal.ui;

import consulo.ui.Breadcrumbs;
import consulo.ui.TextItemRenderer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BreadcrumbClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-26
 */
public class HeadlessBreadcrumbs<E> extends HeadlessComponentBase implements Breadcrumbs<E> {
    private final List<E> myItems = new ArrayList<>();
    private TextItemRenderer<E> myRenderer = TextItemRenderer.defaultRenderer();

    @RequiredUIAccess
    @Override
    public void setItems(List<? extends E> items) {
        myItems.clear();
        myItems.addAll(items);
    }

    @Override
    public List<E> getItems() {
        return List.copyOf(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        myRenderer = renderer;
    }

    public TextItemRenderer<E> getRenderer() {
        return myRenderer;
    }

    /** What a click on the crumb of {@code item} does in a real frontend. */
    @RequiredUIAccess
    public void click(E item) {
        getListenerDispatcher(BreadcrumbClickEvent.class).onEvent(new BreadcrumbClickEvent<>(this, item));
    }
}
