// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.navbar.ui;

import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.navigationBar.model.NavBarPopupItem;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBCurrentTheme;

import javax.swing.JList;

final class NavBarPopupListCellRenderer extends ColoredListCellRenderer<NavBarPopupItem> {
    NavBarPopupListCellRenderer(boolean floating) {
    }

    @Override
    protected void customizeCellRenderer(
        JList<? extends NavBarPopupItem> list,
        NavBarPopupItem value,
        int index,
        boolean selected,
        boolean hasFocus
    ) {
        NavBarItemPresentationData presentation = value.getPresentation();
        // the same cell paddings as in the action list popups; SimpleColoredComponent paints by ipad,
        // border insets would only grow the preferred size without moving the content
        setIpad(JBCurrentTheme.ActionsList.cellPadding());
        setIcon(presentation.icon());

        SimpleTextAttributes attributes =
            presentation.textAttributes() != null ? presentation.textAttributes() : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        String text = presentation.popupText() != null ? presentation.popupText() : presentation.text();
        if (selected) {
            // let the list UI apply the default selection foreground
            append(text);
        }
        else {
            append(text, new SimpleTextAttributes(attributes.getStyle(), attributes.getFgColor(), attributes.getWaveColor()));
        }
    }
}
