// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.ide.actions.Switcher.SwitcherPanel.RECENT_LOCATIONS;
import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

/**
 * @author Konstantin Bulenkov
 */
class SwitcherToolWindowsListRenderer extends ColoredListCellRenderer<Object> {
    private final SpeedSearchBase mySpeedSearch;
    private final Map<ToolWindow, String> myShortcuts;
    private final boolean myPinned;
    private Supplier<Boolean> myShowEdited;

    private boolean hide = false;

    SwitcherToolWindowsListRenderer(
        SpeedSearchBase speedSearch,
        Map<ToolWindow, String> shortcuts,
        boolean pinned,
        @Nonnull Supplier<Boolean> showEdited
    ) {
        mySpeedSearch = speedSearch;
        myShortcuts = shortcuts;
        myPinned = pinned;
        myShowEdited = showEdited;
    }

    @Override
    @RequiredUIAccess
    protected void customizeCellRenderer(@Nonnull JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
        setBorder(JBCurrentTheme.listCellBorderFull());

        String nameToMatch = "";
        if (value instanceof ToolWindow tw) {
            hide = false;
            setIcon(getIcon(tw));

            nameToMatch = tw.getDisplayName().getValue();
            String shortcut = myShortcuts.get(tw);
            String name;
            if (myPinned || shortcut == null) {
                name = nameToMatch;
            }
            else {
                append(shortcut, new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE, null));
                name = ": " + nameToMatch;
            }

            append(name);
        }
        else if (value == RECENT_LOCATIONS) {
            String label = Switcher.SwitcherPanel.getRecentLocationsLabel(myShowEdited);
            nameToMatch = label;

            ShortcutSet shortcuts = getActiveKeymapShortcuts(RecentLocationsAction.RECENT_LOCATIONS_ACTION_ID);
            append(label);

            if (!myShowEdited.get()) {
                append(" ");
                append(KeymapUtil.getShortcutsText(shortcuts.getShortcuts()), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        }

        if (mySpeedSearch != null && mySpeedSearch.isPopupActive()) {
            hide = mySpeedSearch.matchingFragments(nameToMatch) == null && !StringUtil.isEmpty(mySpeedSearch.getEnteredPrefix());
        }
    }

    @Override
    protected void doPaint(Graphics2D g) {
        GraphicsConfig config = new GraphicsConfig(g);
        if (hide) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        }
        super.doPaint(g);
        config.restore();
    }

    @RequiredUIAccess
    private static Image getIcon(ToolWindow toolWindow) {
        Image icon = toolWindow.getIcon();
        return icon == null ? PlatformIconGroup.actionsHelp() : icon;
    }
}
