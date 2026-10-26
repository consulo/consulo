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
package consulo.desktop.qt.ui.impl.breadcrumbs;

import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.desktop.qt.ui.impl.DesktopQtTextItemPresentation;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Breadcrumbs;
import consulo.ui.Rectangle2D;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRenderer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BreadcrumbClickEvent;
import consulo.ui.event.details.InputDetails;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QAction;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QLabel;
import io.qt.widgets.QMenu;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QToolButton;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat tool buttons with separators, placed by hand: when the row is too narrow, the first crumb and as many trailing
 * ones as fit stay visible and the rest go behind an ellipsis button that lists them in a menu - the same rule the awt
 * frontend follows.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public class DesktopQtBreadcrumbsImpl<E> extends QtComponentDelegate<DesktopQtBreadcrumbsImpl<E>.CrumbsWidget> implements Breadcrumbs<E> {
    private static final String SEPARATOR = "›";
    private static final String ELLIPSIS = "…";

    public class CrumbsWidget extends QWidget {
        private final List<QToolButton> myCrumbs = new ArrayList<>();
        private final List<QLabel> mySeparators = new ArrayList<>();
        private final List<E> myCrumbItems = new ArrayList<>();
        private final List<Integer> myHidden = new ArrayList<>();
        private final QToolButton myEllipsis;
        private final QLabel myEllipsisSeparator;

        public CrumbsWidget(@Nullable QWidget parent) {
            super(parent);
            setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Fixed);

            myEllipsis = new QToolButton(this);
            myEllipsis.setText(ELLIPSIS);
            myEllipsis.setAutoRaise(true);
            myEllipsis.clicked.connect(() -> showHidden());
            myEllipsisSeparator = createSeparator(this);
        }

        void setCrumbs(List<E> items) {
            for (QToolButton crumb : myCrumbs) {
                crumb.dispose();
            }
            for (QLabel separator : mySeparators) {
                separator.dispose();
            }
            myCrumbs.clear();
            mySeparators.clear();
            myCrumbItems.clear();

            for (int i = 0; i < items.size(); i++) {
                E item = items.get(i);
                if (i > 0) {
                    mySeparators.add(createSeparator(this));
                }

                DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
                myRenderer.render(presentation, RenderItem.of(item, false));

                QToolButton crumb = new QToolButton(this);
                crumb.setAutoRaise(true);
                crumb.setToolButtonStyle(Qt.ToolButtonStyle.ToolButtonTextBesideIcon);
                crumb.setText(presentation.toString());
                if (presentation.getImage() != null) {
                    crumb.setIcon(toHostQIcon(presentation.getImage()));
                }
                String tooltip = presentation.getTooltip().get();
                if (!tooltip.isEmpty()) {
                    crumb.setToolTip(tooltip);
                }
                crumb.clicked.connect(() -> fire(item, DesktopQtInputDetails.mouseAtCursor(crumb)));

                myCrumbs.add(crumb);
                myCrumbItems.add(item);
            }

            updateGeometry();
            relayout();
        }

        @Override
        public QSize sizeHint() {
            int width = 0;
            int height = myEllipsis.sizeHint().height();
            for (QWidget child : orderedChildren()) {
                QSize hint = child.sizeHint();
                width += hint.width();
                height = Math.max(height, hint.height());
            }
            return new QSize(width, height);
        }

        @Override
        public QSize minimumSizeHint() {
            if (myCrumbs.isEmpty()) {
                return new QSize(0, sizeHint().height());
            }
            int width = myCrumbs.get(myCrumbs.size() - 1).minimumSizeHint().width();
            if (myCrumbs.size() > 1) {
                width += myEllipsis.sizeHint().width() + myEllipsisSeparator.sizeHint().width();
            }
            return new QSize(width, sizeHint().height());
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);
            relayout();
        }

        private void relayout() {
            myEllipsis.hide();
            myEllipsisSeparator.hide();
            for (QWidget child : orderedChildren()) {
                child.hide();
            }
            myHidden.clear();
            if (myCrumbs.isEmpty()) {
                return;
            }

            int x = 0;
            int height = height();
            for (QWidget child : fit(width())) {
                QSize hint = child.sizeHint();
                int childWidth = Math.min(hint.width(), Math.max(0, width() - x));
                child.setGeometry(x, (height - hint.height()) / 2, childWidth, hint.height());
                child.show();
                x += childWidth;
            }
        }

        /**
         * Everything when it fits; otherwise the first crumb, an ellipsis and the longest tail that still fits, and
         * when even that is too wide - the ellipsis and the last crumb, which is then clipped.
         */
        private List<QWidget> fit(int available) {
            List<QWidget> all = orderedChildren();
            if (widthOf(all) <= available || myCrumbs.size() == 1) {
                return all;
            }

            int last = myCrumbs.size() - 1;
            int ellipsisWidth = myEllipsis.sizeHint().width() + myEllipsisSeparator.sizeHint().width();
            int headWidth = myCrumbs.get(0).sizeHint().width() + mySeparators.get(0).sizeHint().width();

            int tailStart = last;
            int tailWidth = myCrumbs.get(last).sizeHint().width();
            while (tailStart > 1) {
                int extra = myCrumbs.get(tailStart - 1).sizeHint().width() + mySeparators.get(tailStart - 1).sizeHint().width();
                if (headWidth + ellipsisWidth + tailWidth + extra > available) {
                    break;
                }
                tailWidth += extra;
                tailStart--;
            }

            List<QWidget> row = new ArrayList<>();
            boolean keepHead = headWidth + ellipsisWidth + tailWidth <= available;
            int hiddenFrom = keepHead ? 1 : 0;
            if (keepHead) {
                row.add(myCrumbs.get(0));
                row.add(mySeparators.get(0));
            }
            if (hiddenFrom < tailStart) {
                row.add(myEllipsis);
                row.add(myEllipsisSeparator);
                for (int i = hiddenFrom; i < tailStart; i++) {
                    myHidden.add(i);
                }
            }
            for (int i = tailStart; i <= last; i++) {
                if (i > tailStart) {
                    row.add(mySeparators.get(i - 1));
                }
                row.add(myCrumbs.get(i));
            }
            return row;
        }

        private List<QWidget> orderedChildren() {
            List<QWidget> all = new ArrayList<>();
            for (int i = 0; i < myCrumbs.size(); i++) {
                if (i > 0) {
                    all.add(mySeparators.get(i - 1));
                }
                all.add(myCrumbs.get(i));
            }
            return all;
        }

        private static int widthOf(List<QWidget> widgets) {
            int width = 0;
            for (QWidget widget : widgets) {
                width += widget.sizeHint().width();
            }
            return width;
        }

        private void showHidden() {
            if (myHidden.isEmpty()) {
                return;
            }
            QMenu menu = new QMenu(this);
            menu.setAttribute(Qt.WidgetAttribute.WA_DeleteOnClose, true);
            for (int index : myHidden) {
                QToolButton crumb = myCrumbs.get(index);
                E item = myCrumbItems.get(index);
                QAction action = menu.addAction(crumb.icon(), crumb.text());
                action.triggered.connect(checked -> fire(item, DesktopQtInputDetails.mouseAtCursor(this)));
            }
            menu.popup(myEllipsis.mapToGlobal(new QPoint(0, myEllipsis.height())));
        }
    }

    private final List<E> myItems = new ArrayList<>();
    private TextItemRenderer<E> myRenderer = TextItemRenderer.defaultRenderer();

    @Override
    protected CrumbsWidget createQt(QWidget parent) {
        return new CrumbsWidget(parent);
    }

    @Override
    protected void initialize(CrumbsWidget component) {
        component.setCrumbs(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<? extends E> items) {
        myItems.clear();
        myItems.addAll(items);
        rebuild();
    }

    @Override
    public List<E> getItems() {
        return List.copyOf(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setRenderer(TextItemRenderer<E> renderer) {
        myRenderer = renderer;
        rebuild();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Rectangle2D getItemBounds(E item) {
        CrumbsWidget widget = toQtComponent();
        if (widget == null || widget.isDisposed()) {
            return null;
        }
        int index = widget.myCrumbItems.indexOf(item);
        if (index < 0) {
            return null;
        }
        QWidget shown = widget.myHidden.contains(index) ? widget.myEllipsis : widget.myCrumbs.get(index);
        if (!shown.isVisible()) {
            return null;
        }
        QRect geometry = shown.geometry();
        return new Rectangle2D(geometry.x(), geometry.y(), geometry.width(), geometry.height());
    }

    private void rebuild() {
        CrumbsWidget widget = toQtComponent();
        if (widget != null && !widget.isDisposed()) {
            widget.setCrumbs(myItems);
        }
    }

    private void fire(E item, InputDetails details) {
        getListenerDispatcher(BreadcrumbClickEvent.class).onEvent(new BreadcrumbClickEvent<>(this, item, details));
    }

    private static QLabel createSeparator(QWidget parent) {
        QLabel separator = new QLabel(SEPARATOR, parent);
        separator.setEnabled(false);
        separator.setContentsMargins(2, 0, 2, 0);
        return separator;
    }
}
