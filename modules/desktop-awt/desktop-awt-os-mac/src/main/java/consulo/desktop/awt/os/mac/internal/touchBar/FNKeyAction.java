// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class FNKeyAction extends DumbAwareAction {
    private static final boolean SHOW_ACTION_TEMPLATE_TEXT = Boolean.getBoolean("touchbar.fn.mode.show.template");

    private final int myFN;
    private final Map<Integer, String[]> myCache = new HashMap<>();

    private AnAction myAction; // particular action (from keymap for given modifiers) calculated in last update
    private boolean myIsActionDisabled;

    @Nullable
    private String  [] getActionsIds(int modifiers) {
        final KeymapManager manager = KeymapManager.getInstance();

        final @Nonnull Keymap keymap = manager.getActiveKeymap();

        String[] result = myCache.get(modifiers);
        if (result != null) {
            return result;
        }
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F1 + myFN - 1, modifiers);
        result = keymap.getActionIds(keyStroke);
        myCache.put(modifiers, result);
        return result;
    }

    FNKeyAction(int FN) {
        myFN = Math.max(1, Math.min(FN, 12));

        // TODO: clear cache when keymap changes (or FN-shortcut changes)
        // KeymapManagerEx.getInstanceEx().addWeakListener(new MyKeymapManagerListener);
    }

    int getFN() {
        return myFN;
    }

    boolean isActionDisabled() {
        return myIsActionDisabled;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (myAction == null || myIsActionDisabled) {
            Helpers.emulateKeyPress(KeyEvent.VK_F1 + myFN - 1);
            return;
        }
        myAction.actionPerformed(e);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true); // FN-keys are always enabled and visible
        e.getPresentation().setText("");
        myIsActionDisabled = false;
        myAction = null;

        final String[] ids = getActionsIds(TouchBarsManager.getLastModifiersEx());
        if (ids == null || ids.length < 1) {
            return;
        }

        int c = 0;
        myAction = e.getActionManager().getAction(ids[c]);
        while (myAction == null && c + 1 < ids.length) {
            ++c;
            e.getActionManager().getAction(ids[c]);
        }

        if (myAction == null) {
            return;
        }

        myAction.update(e);
        myIsActionDisabled = !e.getPresentation().isEnabled();
        e.getPresentation().setEnabledAndVisible(true); // FN-keys are always enabled and visible

        final String text = e.getPresentation().getText();
        if (SHOW_ACTION_TEMPLATE_TEXT || text == null || text.isEmpty()) {
            // replace with template presentation text
            e.getPresentation().setText(myAction.getTemplateText());
        }
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return myAction == null ? ActionUpdateThread.BGT : myAction.getActionUpdateThread();
    }
}
