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
import consulo.util.lang.Pair;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class DesktopQtLayoutComponent<C extends LayoutConstraint, LayoutData> extends QtComponentDelegate<QWidget>
    implements Layout<C> {
    private static final String ourNullMapper = "____null____";

    /**
     * What this container holds, in the order it was filled, bound or not. The children are kept rather than
     * handed over to the widget once: a container is bound more than once - closing a dialog takes the whole tree
     * of widgets under it down and reopening builds a new one - and children remembered only until the first bind
     * would leave every later one empty.
     */
    private final List<Pair<QtComponentDelegate<?>, Object>> myChildren = new ArrayList<>();

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
        for (Pair<QtComponentDelegate<?>, Object> pair : myChildren) {
            attachChild(pair.getFirst(), pair.getSecond());
        }
    }

    protected abstract @Nullable QLayout createLayout();

    /**
     * Places an already bound child inside this container. The default hands it to the {@link QLayout} of the
     * widget; containers which manage children on their own - a splitter or a scroll area - answer differently.
     */
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        if (!isAlive(myComponent)) {
            return;
        }

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
        if (!isAlive(widget) || !isAlive(myComponent)) {
            return;
        }

        QLayout layout = myComponent.layout();
        if (layout != null) {
            layout.removeWidget(widget);
        }
    }

    /**
     * A qt object outlives the native one it stands for: the widget of a container torn down by qt itself -
     * a closed window disposing its whole tree - is still a live java reference, and every call on it throws
     * {@link io.qt.QNoNativeResourcesException}. Anything reaching a widget after a disposal it did not
     * perform has to ask first.
     */
    protected static boolean isAlive(@Nullable QWidget widget) {
        return widget != null && !widget.isDisposed();
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        for (Pair<QtComponentDelegate<?>, Object> pair : myChildren) {
            pair.getFirst().disposeQt();
        }

        myChildren.clear();
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        if (!myChildren.removeIf(pair -> pair.getFirst() == delegate)) {
            return;
        }

        detach(delegate);

        delegate.setParent(null);
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        for (Pair<QtComponentDelegate<?>, Object> pair : new ArrayList<>(myChildren)) {
            consumer.accept(pair.getFirst());
        }
    }

    /**
     * The widgets go, the children stay: what this container holds is the same list of components afterwards, and
     * binding it again is what gives each of them a widget over.
     */
    @Override
    public void disposeQt() {
        for (Pair<QtComponentDelegate<?>, Object> pair : myChildren) {
            pair.getFirst().disposeQt();
        }

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

        String key = mapperOf(layoutData);

        // a layout which names its slots replaces whatever sat in the one being filled, and so does a layout
        // holding a single child. A stacking layout files every child under the same mapper and must keep
        // them all - evicting by key there would drop everything added before
        boolean replaces = layoutData != null || isSingleChild();

        if (replaces) {
            for (Iterator<Pair<QtComponentDelegate<?>, Object>> iterator = myChildren.iterator(); iterator.hasNext(); ) {
                Pair<QtComponentDelegate<?>, Object> oldPair = iterator.next();
                if (!key.equals(mapperOf(oldPair.getSecond()))) {
                    continue;
                }

                iterator.remove();

                if (myComponent != null) {
                    detach(oldPair.getFirst());
                }

                oldPair.getFirst().setParent(null);
            }
        }

        // a null child empties the slot - a splitter of the tool window panel is cleared this way when the
        // window at the anchor is hidden, and the status bar drops its progress the same way
        if (delegate == null) {
            return;
        }

        myChildren.add(Pair.create(delegate, layoutData));

        if (myComponent != null) {
            attachChild(delegate, layoutData);
        }
    }

    private static String mapperOf(@Nullable Object layoutData) {
        return layoutData == null ? ourNullMapper : layoutData.toString();
    }
}
