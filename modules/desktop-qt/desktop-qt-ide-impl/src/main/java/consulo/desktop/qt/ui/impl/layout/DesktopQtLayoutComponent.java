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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class DesktopQtLayoutComponent<C extends LayoutConstraint, LayoutData> extends QtComponentDelegate<QWidget>
    implements Layout<C> {
    private static final String ourNullMapper = "____null____";

    private final List<Pair<QtComponentDelegate<?>, Object>> myComponents = new ArrayList<>();

    private final MultiMap<String, Pair<QtComponentDelegate<?>, Object>> myMappedComponents = new MultiMap<>();

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget widget = new QWidget(parent);

        QLayout layout = createLayout();
        if (layout != null) {
            layout.setContentsMargins(0, 0, 0, 0);
            widget.setLayout(layout);
        }

        return widget;
    }

    @Override
    protected void initialize(QWidget component) {
        for (Pair<QtComponentDelegate<?>, Object> pair : myComponents) {
            attachChild(pair.getFirst(), pair.getSecond());

            String key = pair.getSecond() == null ? ourNullMapper : pair.getSecond().toString();
            myMappedComponents.putValue(key, pair);
        }

        myComponents.clear();
    }

    protected abstract @Nullable QLayout createLayout();

    /**
     * Places an already bound child inside this container. The default hands it to the {@link QLayout} of the
     * widget; containers which manage children on their own - a splitter or a scroll area - answer differently.
     */
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QLayout layout = myComponent.layout();
        if (layout != null) {
            layout.addWidget(child.toQtComponent());
        }
        else {
            child.toQtComponent().setParent(myComponent);
        }
    }

    private void attachChild(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        child.setParent(this);
        child.bind(myComponent, layoutData);

        attach(child, layoutData);
    }

    /**
     * Takes a bound child back out of this container, the counterpart of {@link #attach}. The widget itself is
     * disposed by the caller, but a {@link QLayout} keeps an item of its own for every widget it was given and
     * that item has to be dropped first.
     */
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (widget == null || myComponent == null) {
            return;
        }

        QLayout layout = myComponent.layout();
        if (layout != null) {
            layout.removeWidget(widget);
        }
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        for (Pair<QtComponentDelegate<?>, Object> pair : myComponents) {
            pair.getFirst().disposeQt();
        }
        myComponents.clear();

        for (Pair<QtComponentDelegate<?>, Object> pair : myMappedComponents.values()) {
            pair.getFirst().disposeQt();
        }
        myMappedComponents.clear();
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        boolean found = myComponents.removeIf(pair -> pair.getFirst() == delegate);

        for (Iterator<Map.Entry<String, Collection<Pair<QtComponentDelegate<?>, Object>>>> it =
             myMappedComponents.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Collection<Pair<QtComponentDelegate<?>, Object>>> entry = it.next();

            found |= entry.getValue().removeIf(pair -> pair.getFirst() == delegate);

            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }

        if (!found) {
            return;
        }

        detach(delegate);

        delegate.setParent(null);
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        for (Pair<QtComponentDelegate<?>, Object> pair : new ArrayList<>(myComponents)) {
            consumer.accept(pair.getFirst());
        }

        for (Pair<QtComponentDelegate<?>, Object> pair : new ArrayList<>(myMappedComponents.values())) {
            consumer.accept(pair.getFirst());
        }
    }

    @Override
    public void disposeQt() {
        for (Pair<QtComponentDelegate<?>, Object> pair : myMappedComponents.values()) {
            pair.getFirst().disposeQt();
        }

        myComponents.addAll(myMappedComponents.values());
        myMappedComponents.clear();

        super.disposeQt();
    }

    /**
     * Whether this layout holds one child at a time, so that adding replaces what was there. A layout which
     * stacks its children answers false and keeps every one of them.
     */
    protected boolean isSingleChild() {
        return false;
    }

    public LayoutData convertConstraintsToLayoutData(C constraint) {
        return null;
    }

    @Override
    public Layout<C> add(Component component, C constraint) {
        addImpl(component, convertConstraintsToLayoutData(constraint));
        return this;
    }

    protected void addImpl(@Nullable Component component, @Nullable Object layoutData) {
        QtComponentDelegate<?> delegate = (QtComponentDelegate<?>) component;

        String key = layoutData == null ? ourNullMapper : layoutData.toString();

        // a layout which names its slots replaces whatever sat in the one being filled, and so does a layout
        // holding a single child. A stacking layout files every child under the same mapper and must keep
        // them all - evicting by key there would drop everything added before
        boolean replaces = layoutData != null || isSingleChild();

        if (myComponent != null) {
            if (replaces) {
                Collection<Pair<QtComponentDelegate<?>, Object>> old = myMappedComponents.remove(key);
                if (old != null) {
                    for (Pair<QtComponentDelegate<?>, Object> oldPair : old) {
                        detach(oldPair.getFirst());

                        oldPair.getFirst().setParent(null);
                    }
                }
            }

            // a null child empties the slot - a splitter of the tool window panel is cleared this way when the
            // window at the anchor is hidden, and the status bar drops its progress the same way
            if (delegate == null) {
                return;
            }

            myMappedComponents.putValue(key, Pair.create(delegate, layoutData));

            attachChild(delegate, layoutData);
        }
        else if (delegate != null) {
            myComponents.add(Pair.create(delegate, layoutData));
        }
        else if (replaces) {
            myComponents.removeIf(pair -> key.equals(pair.getSecond() == null ? ourNullMapper : pair.getSecond().toString()));
        }
    }
}
