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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.FlameGraph;
import consulo.ui.chart.FlameGraphDoubleClickEvent;
import consulo.ui.chart.FlameGraphModel;
import consulo.ui.chart.FlameGraphOrientation;
import consulo.ui.chart.FlameGraphSelectEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.impl.chart.FlameGraphIndex;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class HeadlessFlameGraph<E> extends HeadlessComponentBase implements FlameGraph<E> {
    private final FlameGraphModel<E> myModel;
    private final FlameGraphIndex<E> myIndex;

    private FlameGraphOrientation myOrientation = FlameGraphOrientation.FLAME;
    private @Nullable E myFocused;
    private @Nullable E mySelected;
    private @Nullable Predicate<E> myHighlight;

    public HeadlessFlameGraph(FlameGraphModel<E> model) {
        myModel = model;
        myIndex = new FlameGraphIndex<>(model);
    }

    public FlameGraphIndex<E> getIndex() {
        return myIndex;
    }

    public boolean isDimmed(E value) {
        Predicate<E> highlight = myHighlight;
        return highlight != null && !highlight.test(value);
    }

    @RequiredUIAccess
    public void userSelect(@Nullable E value, InputDetails details) {
        mySelected = myIndex.retain(value);
        getListenerDispatcher(FlameGraphSelectEvent.class).onEvent(new FlameGraphSelectEvent<>(this, mySelected, details));
    }

    @RequiredUIAccess
    public void userDoubleClick(E value, InputDetails details) {
        if (myIndex.contains(value)) {
            getListenerDispatcher(FlameGraphDoubleClickEvent.class).onEvent(new FlameGraphDoubleClickEvent<>(this, value, details));
        }
    }

    @Override
    public FlameGraphModel<E> getModel() {
        return myModel;
    }

    @RequiredUIAccess
    @Override
    public void setOrientation(FlameGraphOrientation orientation) {
        myOrientation = orientation;
    }

    @Override
    public FlameGraphOrientation getOrientation() {
        return myOrientation;
    }

    @RequiredUIAccess
    @Override
    public void focus(@Nullable E node) {
        myFocused = myIndex.retain(node);
    }

    @Override
    public @Nullable E getFocused() {
        return myFocused;
    }

    @RequiredUIAccess
    @Override
    public void setHighlight(@Nullable Predicate<E> filter) {
        myHighlight = filter;
    }

    @Override
    public @Nullable E getSelectedValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    public void refresh() {
        myIndex.rebuild();
        myFocused = myIndex.retain(myFocused);
        mySelected = myIndex.retain(mySelected);
    }
}
