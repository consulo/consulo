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
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QLayout;
import io.qt.widgets.QSplitter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtThreeComponentSplitLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object>
    implements ThreeComponentSplitLayout {
    private static final String[] ourSlots = {"first", "center", "second"};

    private static final int ourCenterSlot = 1;

    private class QtSplitter extends QSplitter {
        QtSplitter(Qt.Orientation orientation) {
            super(orientation);
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);

            applyWeights(event.size().width(), event.size().height());
        }
    }

    private final SplitLayoutPosition myPosition;

    /**
     * What is inside the splitter right now, by slot. A {@link QSplitter} only knows the order its panes were
     * handed to it, so the slot a pane belongs to has to be tracked here to place a slot which is filled later
     * ahead of one which was filled first.
     */
    private final QtComponentDelegate<?>[] myAttached = new QtComponentDelegate<?>[ourSlots.length];

    private final double[] myWeights = new double[ourSlots.length];

    private final boolean[] myPendingWeights = new boolean[ourSlots.length];

    public DesktopQtThreeComponentSplitLayoutImpl(SplitLayoutPosition position) {
        myPosition = position;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QSplitter splitter = new QtSplitter(
            myPosition == SplitLayoutPosition.VERTICAL ? Qt.Orientation.Vertical : Qt.Orientation.Horizontal
        );
        splitter.setHandleWidth(1);
        splitter.setChildrenCollapsible(false);
        return splitter;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        int slot = slotOf(layoutData);

        QSplitter splitter = (QSplitter) myComponent;

        myAttached[slot] = child;

        QWidget widget = child.toQtComponent();
        if (myPosition == SplitLayoutPosition.VERTICAL) {
            widget.setMinimumHeight(1);
        }
        else {
            widget.setMinimumWidth(1);
        }

        splitter.insertWidget(indexOfSlot(slot), widget);

        applyStretch();

        if (slot != ourCenterSlot && myWeights[slot] > 0) {
            myPendingWeights[slot] = true;

            if (splitter.isVisible()) {
                applyWeights(splitter.width(), splitter.height());
            }
        }
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        for (int slot = 0; slot < myAttached.length; slot++) {
            if (myAttached[slot] == child) {
                myAttached[slot] = null;
            }
        }
    }

    private static int slotOf(@Nullable Object layoutData) {
        String key = String.valueOf(layoutData);
        for (int slot = 0; slot < ourSlots.length; slot++) {
            if (ourSlots[slot].equals(key)) {
                return slot;
            }
        }
        return ourCenterSlot;
    }

    private int indexOfSlot(int slot) {
        int index = 0;
        for (int i = 0; i < slot; i++) {
            if (myAttached[i] != null) {
                index++;
            }
        }
        return index;
    }

    /**
     * Without a stretch a splitter shares itself out by the size hints of its panes, and a tool window - which
     * asks to expand - then takes the whole frame from the editor. Only the center grows, the sides keep the
     * width they were given, which is how the awt and web layouts behave.
     */
    private void applyStretch() {
        QSplitter splitter = (QSplitter) myComponent;

        int index = 0;
        for (int slot = 0; slot < myAttached.length; slot++) {
            if (myAttached[slot] != null) {
                splitter.setStretchFactor(index++, slot == ourCenterSlot ? 1 : 0);
            }
        }
    }

    private void applyWeights(int width, int height) {
        if (!(myComponent instanceof QSplitter splitter)) {
            return;
        }

        List<Integer> sizes = new ArrayList<>(splitter.sizes());
        if (sizes.size() != splitter.count()) {
            return;
        }

        boolean vertical = myPosition == SplitLayoutPosition.VERTICAL;
        int total = (vertical ? height : width) - splitter.handleWidth() * Math.max(0, splitter.count() - 1);
        if (total <= 0) {
            return;
        }

        boolean changed = false;
        int sides = 0;
        for (int slot = 0; slot < myAttached.length; slot++) {
            if (slot == ourCenterSlot || myAttached[slot] == null) {
                continue;
            }

            int index = indexOfSlot(slot);
            if (myPendingWeights[slot]) {
                myPendingWeights[slot] = false;
                sizes.set(index, (int) (total * myWeights[slot]));
                changed = true;
            }
            else if (sizes.get(index) <= 0) {
                QSize hint = splitter.widget(index).sizeHint();
                sizes.set(index, vertical ? hint.height() : hint.width());
            }
            sides += sizes.get(index);
        }

        if (!changed) {
            return;
        }

        if (myAttached[ourCenterSlot] != null) {
            sizes.set(indexOfSlot(ourCenterSlot), Math.max(1, total - sides));
        }

        splitter.setSizes(sizes);
    }

    public void setFirstWeight(double weight) {
        myWeights[0] = weight;
    }

    public void setSecondWeight(double weight) {
        myWeights[2] = weight;
    }

    @RequiredUIAccess
    private void setSlotComponent(int slot, @Nullable Component component) {
        // the pane which sat here is disposed by addImpl, which takes it out of the splitter as well - the slot
        // has to be empty before that, so whatever comes next is counted against what is really inside
        myAttached[slot] = null;

        addImpl(component, ourSlots[slot]);
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        super.removeAll();

        Arrays.fill(myAttached, null);
    }

    @Override
    public void disposeQt() {
        super.disposeQt();

        Arrays.fill(myAttached, null);
        Arrays.fill(myPendingWeights, false);
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setFirstComponent(@Nullable Component component) {
        setSlotComponent(0, component);
        return this;
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setCenterComponent(@Nullable Component component) {
        setSlotComponent(ourCenterSlot, component);
        return this;
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setSecondComponent(@Nullable Component component) {
        setSlotComponent(2, component);
        return this;
    }
}
