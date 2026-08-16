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
import consulo.ui.layout.TwoComponentSplitLayout;
import io.qt.core.Qt;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QLayout;
import io.qt.widgets.QSplitter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTwoComponentSplitLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object>
    implements TwoComponentSplitLayout {
    private static final String[] ourSlots = {"first", "second"};

    /**
     * A splitter which keeps the proportion it was given. Qt hands its panes their sizes in pixels and clamps
     * every one of them to the minimum the pane needs, so a ratio can only be turned into sizes once the
     * splitter knows how big it is - and again whenever that changes.
     */
    private class QtSplitter extends QSplitter {
        QtSplitter(Qt.Orientation orientation) {
            super(orientation);
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);

            if (!myUserMoved) {
                applyProportion();
            }
        }
    }

    private final SplitLayoutPosition myPosition;

    private final QtComponentDelegate<?>[] myAttached = new QtComponentDelegate<?>[ourSlots.length];

    private int myProportion = 50;

    private boolean myUserMoved;

    public DesktopQtTwoComponentSplitLayoutImpl(SplitLayoutPosition position) {
        myPosition = position;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QSplitter splitter = new QtSplitter(
            myPosition == SplitLayoutPosition.VERTICAL ? Qt.Orientation.Vertical : Qt.Orientation.Horizontal
        );
        splitter.setHandleWidth(1);
        splitter.setChildrenCollapsible(false);
        splitter.splitterMoved.connect((position, index) -> myUserMoved = true);
        return splitter;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        int slot = ourSlots[1].equals(String.valueOf(layoutData)) ? 1 : 0;

        QSplitter splitter = (QSplitter) myComponent;

        myAttached[slot] = child;

        splitter.insertWidget(slot == 1 && myAttached[0] == null ? 0 : slot, child.toQtComponent());

        applyProportion();
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        for (int slot = 0; slot < myAttached.length; slot++) {
            if (myAttached[slot] == child) {
                myAttached[slot] = null;
            }
        }
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        applyProportion();
    }

    private void applyProportion() {
        if (!(myComponent instanceof QSplitter splitter) || splitter.count() != 2) {
            return;
        }

        int total = myPosition == SplitLayoutPosition.VERTICAL ? splitter.height() : splitter.width();
        if (total <= 0) {
            return;
        }

        int first = total * myProportion / 100;

        splitter.setSizes(List.of(first, total - first));
    }

    @Override
    public void setProportion(int percent) {
        myProportion = percent;
        myUserMoved = false;

        applyProportion();
    }

    @RequiredUIAccess
    private void setSlotComponent(int slot, @Nullable Component component) {
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
    }

    @Override
    @RequiredUIAccess
    public void setFirstComponent(Component component) {
        setSlotComponent(0, component);
    }

    @Override
    @RequiredUIAccess
    public void setSecondComponent(Component component) {
        setSlotComponent(1, component);
    }
}
