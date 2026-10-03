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
package consulo.ui.chart;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public interface FlameGraph<E> extends Component {
    static <E> FlameGraph<E> create(FlameGraphModel<E> model) {
        return UIInternal.get()._Components_flameGraph(model);
    }

    FlameGraphModel<E> getModel();

    @RequiredUIAccess
    void setOrientation(FlameGraphOrientation orientation);

    FlameGraphOrientation getOrientation();

    @RequiredUIAccess
    void focus(@Nullable E node);

    @Nullable
    E getFocused();

    @RequiredUIAccess
    void setHighlight(@Nullable Predicate<E> filter);

    @Nullable
    E getSelectedValue();

    @RequiredUIAccess
    void refresh();

    @SuppressWarnings("unchecked")
    default Disposable addSelectListener(ComponentEventListener<FlameGraph<E>, FlameGraphSelectEvent<E>> listener) {
        return addListener((Class) FlameGraphSelectEvent.class, listener);
    }

    @SuppressWarnings("unchecked")
    default Disposable addDoubleClickListener(ComponentEventListener<FlameGraph<E>, FlameGraphDoubleClickEvent<E>> listener) {
        return addListener((Class) FlameGraphDoubleClickEvent.class, listener);
    }
}
