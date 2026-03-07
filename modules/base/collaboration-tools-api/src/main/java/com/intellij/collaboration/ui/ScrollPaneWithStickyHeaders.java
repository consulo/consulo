// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

@ApiStatus.Internal
public final class ScrollPaneWithStickyHeaders {
    private ScrollPaneWithStickyHeaders() {
    }

    /**
     * @param components List of child components along with sticky flags
     */
    public static @Nonnull JComponent create(@Nonnull List<kotlin.Pair<JComponent, Boolean>> components) {
        NonOpaquePanel stickyLayer = new NonOpaquePanel(new BorderLayout());

        OpaquePanel topStuckPane = new OpaquePanel(new VerticalFlowLayout(0, 0));
        topStuckPane.setBorder(IdeBorderFactory.createBorder(OnePixelDivider.BACKGROUND, SideBorder.BOTTOM));

        OpaquePanel bottomStuckPane = new OpaquePanel(new VerticalFlowLayout(0, 0));
        bottomStuckPane.setBorder(IdeBorderFactory.createBorder(OnePixelDivider.BACKGROUND, SideBorder.BOTTOM));

        NonOpaquePanel scrolledBody = new NonOpaquePanel(new VerticalFlowLayout(0, 0));

        JBScrollPane scrollPane =
            new JBScrollPane(scrolledBody, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        Map<Component, StickyElement> stickyElements = new LinkedHashMap<>();

        for (kotlin.Pair<JComponent, Boolean> pair : components) {
            JComponent component = pair.getFirst();
            boolean isSticky = pair.getSecond();
            if (isSticky) {
                JPanel wrapperInBody = createWrapper();
                wrapperInBody.add(component);
                scrolledBody.add(wrapperInBody);

                JPanel wrapperInTop = createWrapper();
                wrapperInTop.setVisible(false);
                topStuckPane.add(wrapperInTop);

                JPanel wrapperInBottom = createWrapper();
                wrapperInBottom.setVisible(false);
                bottomStuckPane.add(wrapperInBottom);

                StickyElement element = new StickyElement(component, wrapperInBody, wrapperInTop, wrapperInBottom,
                    scrollPane, scrolledBody, stickyElements.values()
                );
                stickyElements.put(wrapperInBody, element);
            }
            else {
                scrolledBody.add(component);
            }
        }

        JBLayeredPane layeredPane = new JBLayeredPane();
        layeredPane.setName("scrollable-sticked-pane");
        layeredPane.setFullOverlayLayout(true);
        scrollPane.setBorder(JBUI.Borders.empty());

        scrollPane.getViewport().addChangeListener(e -> {
            int scrollPosition = scrollPane.getViewport().getViewPosition().y;
            int topLimit = 0;
            int bottomLimit = scrollPane.getHeight();

            List<StickyElement> elements = new ArrayList<>();
            for (Component c : scrolledBody.getComponents()) {
                StickyElement el = stickyElements.get(c);
                if (el != null && el.component.isVisible()) {
                    elements.add(el);
                }
            }

            for (StickyElement element : elements) {
                int elemPosition = element.wrapperBody.getY() - scrollPosition;
                if (elemPosition < topLimit) {
                    element.underTopLimit = false;
                    topLimit += element.wrapperBody.getHeight();
                }
                else {
                    element.underTopLimit = true;
                }
            }

            for (int i = elements.size() - 1; i >= 0; i--) {
                StickyElement element = elements.get(i);
                int elemPosition = element.wrapperBody.getY() + element.wrapperBody.getHeight() - scrollPosition;
                if (elemPosition > bottomLimit) {
                    element.aboveBottomLimit = false;
                    bottomLimit -= element.wrapperBody.getHeight();
                }
                else {
                    element.aboveBottomLimit = true;
                }
                element.move();
            }

            layeredPane.revalidate();
            layeredPane.repaint();
        });

        stickyLayer.add(topStuckPane, BorderLayout.NORTH);
        stickyLayer.add(bottomStuckPane, BorderLayout.SOUTH);

        layeredPane.add(stickyLayer, Integer.valueOf(1));
        layeredPane.add(scrollPane, Integer.valueOf(0));

        return layeredPane;
    }

    private static @Nonnull JPanel createWrapper() {
        NonOpaquePanel panel = new NonOpaquePanel();
        panel.setName("wrapper");
        panel.setBorder(IdeBorderFactory.createBorder(OnePixelDivider.BACKGROUND, SideBorder.TOP));
        return panel;
    }

    private static final class StickyElement {
        final Component component;
        final JPanel wrapperBody;
        final JPanel wrapperTop;
        final JPanel wrapperBottom;
        boolean underTopLimit = true;
        boolean aboveBottomLimit = true;
        private List<StickyElement> beforeElems;

        private final NonOpaquePanel dummy = new NonOpaquePanel();

        StickyElement(
            @Nonnull Component component,
            @Nonnull JPanel wrapperBody,
            @Nonnull JPanel wrapperTop,
            @Nonnull JPanel wrapperBottom,
            @Nonnull JBScrollPane scrollPane,
            @Nonnull JPanel scrolledBody,
            @Nonnull Collection<StickyElement> stickyElems
        ) {
            this.component = component;
            this.wrapperBody = wrapperBody;
            this.wrapperTop = wrapperTop;
            this.wrapperBottom = wrapperBottom;

            MouseAdapter listener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (beforeElems == null) {
                        beforeElems = new ArrayList<>();
                        for (StickyElement el : stickyElems) {
                            if (el == StickyElement.this) {
                                break;
                            }
                            beforeElems.add(el);
                        }
                    }
                    int offset = beforeElems.stream().mapToInt(el -> el.component.getHeight()).sum();
                    scrollPane.getViewport().setViewPosition(new Point(0, wrapperBody.getY() - offset));
                }
            };
            wrapperTop.addMouseListener(listener);
            wrapperBottom.addMouseListener(listener);
        }

        boolean isInTop() {
            return wrapperTop.getComponentCount() > 0;
        }

        boolean isInBottom() {
            return wrapperBottom.getComponentCount() > 0;
        }

        boolean isInBody() {
            return !isInTop() && !isInBottom();
        }

        void move() {
            if (!underTopLimit && !isInTop()) {
                wrapperTop.add(component);
                wrapperTop.setVisible(true);
                wrapperBottom.setVisible(false);
                dummy.setPreferredSize(component.getPreferredSize());
                wrapperBody.add(dummy);
            }
            else if (underTopLimit && aboveBottomLimit && !isInBody()) {
                wrapperTop.setVisible(false);
                wrapperBottom.setVisible(false);
                wrapperBody.remove(dummy);
                wrapperBody.add(component);
            }
            else if (!aboveBottomLimit && !isInBottom()) {
                wrapperBottom.setVisible(true);
                wrapperTop.setVisible(false);
                wrapperBottom.add(component);
                dummy.setPreferredSize(component.getPreferredSize());
                wrapperBody.add(dummy);
            }
        }
    }
}
