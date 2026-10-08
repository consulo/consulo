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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.PlaceholderLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QStackedLayout;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class DesktopQtPlaceholderLayoutImpl extends DesktopQtLayoutComponent<PlaceholderLayout.Layer, PlaceholderLayout.Layer>
    implements PlaceholderLayout {
    private final Map<Layer, QWidget> mySlots = new EnumMap<>(Layer.class);

    private @Nullable QStackedLayout myStackedLayout;

    private BooleanSupplier myPlaceholderVisibility = () -> false;
    private boolean myPlaceholderVisible;

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget widget = new QWidget(parent);

        QStackedLayout stackedLayout = new QStackedLayout(widget);
        stackedLayout.setContentsMargins(0, 0, 0, 0);
        myStackedLayout = stackedLayout;

        mySlots.clear();
        for (Layer layer : Layer.values()) {
            QWidget slot = new QWidget();
            QVBoxLayout slotLayout = new QVBoxLayout(slot);
            slotLayout.setContentsMargins(0, 0, 0, 0);
            slotLayout.setSpacing(0);

            stackedLayout.addWidget(slot);
            mySlots.put(layer, slot);
        }

        showCurrentSlot();
        return widget;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    public Layer convertConstraintsToLayoutData(Layer constraint) {
        return constraint;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        QWidget slot = layoutData instanceof Layer layer ? mySlots.get(layer) : null;
        if (!isAlive(slot)) {
            return;
        }

        QLayout slotLayout = slot.layout();
        if (slotLayout != null) {
            slotLayout.addWidget(child.toQtComponent());
        }
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (!isAlive(widget)) {
            return;
        }

        for (QWidget slot : mySlots.values()) {
            if (isAlive(slot) && widget.parentWidget() == slot) {
                QLayout slotLayout = slot.layout();
                if (slotLayout != null) {
                    slotLayout.removeWidget(widget);
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void setPlaceholderVisibility(BooleanSupplier booleanSupplier) {
        myPlaceholderVisibility = booleanSupplier;
        testPlaceholder();
    }

    @Override
    @RequiredUIAccess
    public boolean testPlaceholder() {
        boolean placeholderVisible = myPlaceholderVisibility.getAsBoolean();
        myPlaceholderVisible = placeholderVisible;
        showCurrentSlot();
        return placeholderVisible;
    }

    private void showCurrentSlot() {
        QStackedLayout stackedLayout = myStackedLayout;
        QWidget slot = mySlots.get(myPlaceholderVisible ? Layer.PLACEHOLDER : Layer.CONTENT);
        if (stackedLayout == null || stackedLayout.isDisposed() || !isAlive(slot)) {
            return;
        }

        stackedLayout.setCurrentWidget(slot);
    }
}
