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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BreadcrumbClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A trail of items from the root to the current one, each of them clickable - a path bar, a navigation bar.
 *
 * <p>When the trail does not fit, the frontend collapses the items between the first and the last ones it can show,
 * so the root and the current item stay visible.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public interface Breadcrumbs<E> extends Component {
    static <E> Breadcrumbs<E> create() {
        return UIInternal.get()._Components_breadcrumbs();
    }

    /**
     * @param items root first, current item last
     */
    @RequiredUIAccess
    void setItems(List<? extends E> items);

    List<E> getItems();

    /**
     * Text and icon of one item. Without a renderer the item is shown by its {@code toString()}.
     */
    @RequiredUIAccess
    void setRenderer(TextItemRenderer<E> renderer);

    /**
     * Where the crumb of {@code item} is drawn, in the coordinates of this component - what a popup offered for the
     * crumb is shown against, see {@link Popup#showAt(Component, int, int, int)}. An item collapsed into the
     * ellipsis answers the ellipsis.
     *
     * @return null when the item is not shown, or when the frontend cannot measure it synchronously
     */
    @RequiredUIAccess
    default @Nullable Rectangle2D getItemBounds(E item) {
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default Disposable addItemClickListener(ComponentEventListener<Breadcrumbs<E>, BreadcrumbClickEvent<E>> listener) {
        return addListener((Class) BreadcrumbClickEvent.class, listener);
    }
}
