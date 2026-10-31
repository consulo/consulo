/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.desktop.awt.wm.impl;

import consulo.desktop.awt.ui.impl.plaf.IslandPainter;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.awt.Divider;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindowType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ContainerEvent;

public final class IslandsUI {
    private IslandsUI() {
    }

    public static boolean isEnabled() {
        return IslandPainter.isEnabled();
    }

    public static Color getMainBackground() {
        return IslandPainter.getMainBackground();
    }

    public static Color getToolWindowBackground() {
        return JBColor.namedColor("ToolWindow.background", UIUtil.getPanelBackground());
    }

    private static final String FLAT_STYLE = "FlatLaf.style";

    private static boolean ourListenerInstalled;

    public static void installToolWindowBackgroundListener() {
        if (ourListenerInstalled) {
            return;
        }
        ourListenerInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == ContainerEvent.COMPONENT_ADDED && event instanceof ContainerEvent containerEvent) {
                Component child = containerEvent.getChild();
                if (containerEvent.getContainer() instanceof CellRendererPane
                    || SwingUtilities.getAncestorOfClass(CellRendererPane.class, child) != null) {
                    return;
                }
                if (SwingUtilities.getAncestorOfClass(DesktopInternalDecorator.class, child) instanceof DesktopInternalDecorator decorator
                    && isIsland(decorator.getToolWindow().getType())) {
                    setToolWindowBackgroundRecursively(child);
                }
            }
        }, AWTEvent.CONTAINER_EVENT_MASK);
    }

    public static boolean isIsland(ToolWindowType type) {
        return type == ToolWindowType.DOCKED || type == ToolWindowType.SLIDING;
    }

    public static void setToolWindowBackgroundRecursively(Component component) {
        setBackgroundFor(component, getToolWindowBackground());
    }

    private static void setBackgroundFor(Component component, Color background) {
        if (component instanceof CellRendererPane) {
            return;
        }
        if (updateBackgroundFor(component)) {
            component.setBackground(background);
        }
        if (component instanceof JTree tree) {
            installTreeStyle(tree);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setBackgroundFor(child, background);
            }
        }
    }

    private static void installTreeStyle(JTree tree) {
        if (tree.getClientProperty(FLAT_STYLE) == null) {
            tree.putClientProperty(FLAT_STYLE, "selectionInsets: 0,6,0,6; selectionArc: 8");
        }
        Border border = tree.getBorder();
        if (border == null || border instanceof UIResource) {
            tree.setBorder(new BorderUIResource.EmptyBorderUIResource(JBUI.insets(4, 12)));
        }
    }

    private static boolean updateBackgroundFor(Component component) {
        if (component instanceof ActionButtonComponent
            || component instanceof AbstractButton
            || component instanceof Divider
            || component instanceof JTextComponent
            || component instanceof JComboBox<?>
            || component instanceof EditorTextField) {
            return false;
        }
        return !(component.isBackgroundSet() && !(component.getBackground() instanceof UIResource));
    }

    public static int getIslandGap() {
        return IslandPainter.getGap();
    }
}
