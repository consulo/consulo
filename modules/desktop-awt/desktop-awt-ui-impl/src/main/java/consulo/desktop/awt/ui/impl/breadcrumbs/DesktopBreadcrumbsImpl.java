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
package consulo.desktop.awt.ui.impl.breadcrumbs;

import consulo.desktop.awt.ui.impl.DesktopTextItemPresentationImpl;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Breadcrumbs;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.RenderItem;
import consulo.ui.TextItemRenderer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BreadcrumbClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A row of crumbs with separators. When the row is too narrow, the first crumb and as many trailing ones as fit stay
 * visible and the rest go behind an ellipsis that lists them in a popup.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public class DesktopBreadcrumbsImpl<E> extends SwingComponentDelegate<DesktopBreadcrumbsImpl<E>.CrumbsPanel> implements Breadcrumbs<E> {
    private static final String SEPARATOR = "›";
    private static final String ELLIPSIS = "…";

    public class CrumbsPanel extends JPanel implements FromSwingComponentWrapper {
        private final List<Crumb> myCrumbs = new ArrayList<>();
        private final List<JBLabel> mySeparators = new ArrayList<>();
        private final JBLabel myEllipsis = new JBLabel(ELLIPSIS);
        private final JBLabel myEllipsisSeparator = createSeparator();
        private final List<Crumb> myHidden = new ArrayList<>();

        public CrumbsPanel() {
            super(null);
            setOpaque(false);

            myEllipsis.setBorder(JBUI.Borders.empty(1, 4));
            myEllipsis.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            myEllipsis.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showHidden();
                }
            });
        }

        @Override
        public Component toUIComponent() {
            return DesktopBreadcrumbsImpl.this;
        }

        void setCrumbs(List<Crumb> crumbs) {
            removeAll();
            myCrumbs.clear();
            mySeparators.clear();
            myCrumbs.addAll(crumbs);
            for (int i = 0; i < crumbs.size(); i++) {
                if (i > 0) {
                    JBLabel separator = createSeparator();
                    mySeparators.add(separator);
                    add(separator);
                }
                add(crumbs.get(i));
            }
            add(myEllipsis);
            add(myEllipsisSeparator);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (isPreferredSizeSet()) {
                return super.getPreferredSize();
            }
            int width = 0;
            int height = 0;
            for (java.awt.Component child : orderedChildren()) {
                Dimension size = child.getPreferredSize();
                width += size.width;
                height = Math.max(height, size.height);
            }
            Insets insets = getInsets();
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        @Override
        public Dimension getMinimumSize() {
            if (isMinimumSizeSet() || myCrumbs.isEmpty()) {
                return super.getMinimumSize();
            }
            Crumb last = myCrumbs.get(myCrumbs.size() - 1);
            int width = last.getMinimumSize().width;
            if (myCrumbs.size() > 1) {
                width += myEllipsis.getPreferredSize().width + myEllipsisSeparator.getPreferredSize().width;
            }
            return new Dimension(width, getPreferredSize().height);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int available = getWidth() - insets.left - insets.right;
            int height = getHeight() - insets.top - insets.bottom;

            for (java.awt.Component child : getComponents()) {
                child.setVisible(false);
            }
            myHidden.clear();
            if (myCrumbs.isEmpty()) {
                return;
            }

            List<java.awt.Component> row = fit(available);
            int x = insets.left;
            for (java.awt.Component child : row) {
                Dimension size = child.getPreferredSize();
                int width = Math.min(size.width, Math.max(0, insets.left + available - x));
                child.setBounds(x, insets.top + (height - size.height) / 2, width, size.height);
                child.setVisible(true);
                x += width;
            }
        }

        /**
         * Everything when it fits; otherwise the first crumb, an ellipsis and the longest tail that still fits, and
         * when even that is too wide - the ellipsis and the last crumb, which is then clipped.
         */
        private List<java.awt.Component> fit(int available) {
            List<java.awt.Component> all = orderedChildren();
            if (widthOf(all) <= available || myCrumbs.size() == 1) {
                return all;
            }

            int last = myCrumbs.size() - 1;
            int ellipsisWidth = myEllipsis.getPreferredSize().width + myEllipsisSeparator.getPreferredSize().width;
            int headWidth = myCrumbs.get(0).getPreferredSize().width + mySeparators.get(0).getPreferredSize().width;

            int tailStart = last;
            int tailWidth = myCrumbs.get(last).getPreferredSize().width;
            while (tailStart > 1) {
                int extra = myCrumbs.get(tailStart - 1).getPreferredSize().width + mySeparators.get(tailStart - 1).getPreferredSize().width;
                if (headWidth + ellipsisWidth + tailWidth + extra > available) {
                    break;
                }
                tailWidth += extra;
                tailStart--;
            }

            List<java.awt.Component> row = new ArrayList<>();
            boolean keepHead = headWidth + ellipsisWidth + tailWidth <= available;
            int hiddenFrom = keepHead ? 1 : 0;
            if (keepHead) {
                row.add(myCrumbs.get(0));
                row.add(mySeparators.get(0));
            }
            if (hiddenFrom < tailStart) {
                row.add(myEllipsis);
                row.add(myEllipsisSeparator);
                myHidden.addAll(myCrumbs.subList(hiddenFrom, tailStart));
            }
            for (int i = tailStart; i <= last; i++) {
                if (i > tailStart) {
                    row.add(mySeparators.get(i - 1));
                }
                row.add(myCrumbs.get(i));
            }
            return row;
        }

        private List<java.awt.Component> orderedChildren() {
            List<java.awt.Component> all = new ArrayList<>();
            for (int i = 0; i < myCrumbs.size(); i++) {
                if (i > 0) {
                    all.add(mySeparators.get(i - 1));
                }
                all.add(myCrumbs.get(i));
            }
            return all;
        }

        private int widthOf(List<java.awt.Component> components) {
            int width = 0;
            for (java.awt.Component component : components) {
                width += component.getPreferredSize().width;
            }
            return width;
        }

        private void showHidden() {
            if (myHidden.isEmpty()) {
                return;
            }
            JPopupMenu menu = new JPopupMenu();
            for (Crumb crumb : myHidden) {
                JMenuItem item = new JMenuItem(crumb.getCharSequence(false).toString(), TargetAWT.to(crumb.getIcon()));
                item.addActionListener(e -> crumb.fire(DesktopAWTInputDetails.convert(item, e)));
                menu.add(item);
            }
            menu.show(myEllipsis, 0, myEllipsis.getHeight());
        }
    }

    private class Crumb extends SimpleColoredComponent {
        private final E myItem;

        Crumb(E item) {
            myItem = item;
            setOpaque(false);
            setIpad(JBUI.insets(1, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            myRenderer.render(new DesktopTextItemPresentationImpl(this), RenderItem.of(item, false));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setOpaque(true);
                    setBackground(UIUtil.getListUnfocusedSelectionBackground());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setOpaque(false);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
                        fire(DesktopAWTInputDetails.convert(Crumb.this, e));
                    }
                }
            });
        }

        void fire(InputDetails details) {
            getListenerDispatcher(BreadcrumbClickEvent.class).onEvent(new BreadcrumbClickEvent<>(DesktopBreadcrumbsImpl.this, myItem, details));
        }
    }

    private final List<E> myItems = new ArrayList<>();
    private TextItemRenderer<E> myRenderer = TextItemRenderer.defaultRenderer();

    @Override
    protected CrumbsPanel createComponent() {
        return new CrumbsPanel();
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
        CrumbsPanel panel = toAWTComponent();
        // bounds are only set by the layout, which a fresh trail has not had yet
        panel.validate();
        for (Crumb crumb : panel.myCrumbs) {
            if (!Objects.equals(crumb.myItem, item)) {
                continue;
            }
            java.awt.Component shown = panel.myHidden.contains(crumb) ? panel.myEllipsis : crumb;
            if (!shown.isVisible()) {
                return null;
            }
            Rectangle bounds = shown.getBounds();
            return new Rectangle2D(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        return null;
    }

    private void rebuild() {
        List<Crumb> crumbs = new ArrayList<>(myItems.size());
        for (E item : myItems) {
            crumbs.add(new Crumb(item));
        }
        toAWTComponent().setCrumbs(crumbs);
    }

    private static JBLabel createSeparator() {
        JBLabel separator = new JBLabel(SEPARATOR);
        separator.setForeground(UIUtil.getContextHelpForeground());
        separator.setBorder(JBUI.Borders.empty(0, 2));
        return separator;
    }
}
