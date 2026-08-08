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

import consulo.ui.annotation.RequiredUIAccess;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Renders an item as a full component, for rows that text alone cannot express.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
@FunctionalInterface
public interface ComponentItemRender<E> {
    /**
     * Builds the component once and rebinds it per item, instead of allocating a fresh one every
     * time.
     * <p/>
     * How often {@code factory} runs is the backend's business - desktop reuses a single component
     * for every row, web needs one per row because each lives in the document. So {@code binder}
     * must set every piece of state it cares about, never assuming a clean component.
     */
    static <E, C extends Component> ComponentItemRender<E> reusable(@RequiredUIAccess Supplier<C> factory,
                                                                    @RequiredUIAccess BiConsumer<C, RenderItem<E>> binder) {
        return new ReusableComponentItemRender<>(factory, binder);
    }

    Component render(RenderItem<E> item);
}
