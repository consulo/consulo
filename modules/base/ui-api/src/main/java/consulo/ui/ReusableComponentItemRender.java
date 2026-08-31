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

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Backends recognize this to apply their own reuse strategy - desktop keeps one component for the
 * whole list, web has to build one per row.
 *
 * @author VISTALL
 * @since 2026-08-02
 * @see ComponentItemRender#reusable(Supplier, BiConsumer)
 */
public final class ReusableComponentItemRender<E, C extends Component> implements ComponentItemRender<E> {
    private final Supplier<C> myFactory;
    private final BiConsumer<C, RenderItem<E>> myBinder;

    ReusableComponentItemRender(Supplier<C> factory, BiConsumer<C, RenderItem<E>> binder) {
        myFactory = factory;
        myBinder = binder;
    }

    public C createComponent() {
        return myFactory.get();
    }

    public void bind(C component, RenderItem<E> item) {
        myBinder.accept(component, item);
    }

    @Override
    public Component render(RenderItem<E> item) {
        C component = myFactory.get();
        myBinder.accept(component, item);
        return component;
    }
}
