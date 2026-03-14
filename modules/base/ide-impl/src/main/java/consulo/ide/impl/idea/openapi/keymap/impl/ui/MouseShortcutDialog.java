// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.AllIcons;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.MouseShortcut;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.ex.keymap.util.KeymapUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

final class MouseShortcutDialog extends ShortcutDialog<MouseShortcut> {
    private final JLabel myText = new JLabel("", SwingConstants.CENTER);

    MouseShortcutDialog(Component component, boolean allowDoubleClick) {
        super(component, KeyMapLocalize.mouseShortcutDialogTitle(), new MouseShortcutPanel(allowDoubleClick));

        myShortcutPanel.add(BorderLayout.NORTH, new JLabel(TargetAWT.to(AllIcons.General.Mouse), SwingConstants.CENTER));
        myShortcutPanel.add(BorderLayout.CENTER, myText);
        myShortcutPanel.setBorder(BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0),
            JBUI.Borders.empty(20)
        ));

        init();
    }

    @Override
    protected String getHelpId() {
        return "preferences.mouse.shortcut";
    }

    @Override
    MouseShortcut toShortcut(Object value) {
        return value instanceof MouseShortcut ? (MouseShortcut) value : null;
    }

    @Override
    void setShortcut(MouseShortcut shortcut) {
        super.setShortcut(shortcut);
        if (shortcut == null) {
            myText.setForeground(UIUtil.getContextHelpForeground());
            myText.setText(KeyMapLocalize.dialogMousePadDefaultText().get());
        }
        else {
            myText.setForeground(UIUtil.getLabelForeground());
            myText.setText(KeyMapLocalize.dialogMousePadShortcutText(KeymapUtil.getMouseShortcutText(shortcut)).get());
        }
    }

    @Override
    @Nonnull
    Collection<String> getConflicts(MouseShortcut shortcut, String actionId, Keymap keymap) {
        return Set.of(keymap.getActionIds(shortcut));
    }
}
