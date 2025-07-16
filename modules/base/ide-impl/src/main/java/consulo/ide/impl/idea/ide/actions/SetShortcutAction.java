// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManager;
import consulo.ide.impl.idea.openapi.keymap.impl.ActionShortcutRestrictions;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

import java.awt.*;

public class SetShortcutAction extends AnAction implements DumbAware {
    public final static Key<AnAction> SELECTED_ACTION = Key.create("SelectedAction");

    public SetShortcutAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        JBPopup seDialog = project.getUserData(SearchEverywhereManager.SEARCH_EVERYWHERE_POPUP);
        if (seDialog == null) {
            return;
        }

        KeymapManager km = KeymapManager.getInstance();
        Keymap activeKeymap = km.getActiveKeymap();
        if (activeKeymap == null) {
            return;
        }

        AnAction action = e.getRequiredData(SELECTED_ACTION);
        Component component = e.getRequiredData(UIExAWTDataKey.CONTEXT_COMPONENT);

        seDialog.cancel();
        String id = ActionManager.getInstance().getId(action);
        KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, component);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Project project = e.getData(Project.KEY);
        JBPopup seDialog = project == null ? null : project.getUserData(SearchEverywhereManager.SEARCH_EVERYWHERE_POPUP);
        if (seDialog == null) {
            presentation.setEnabled(false);
            return;
        }

        KeymapManager km = KeymapManager.getInstance();
        Keymap activeKeymap = km.getActiveKeymap();
        if (activeKeymap == null) {
            presentation.setEnabled(false);
            return;
        }

        presentation.setEnabled(e.hasData(SELECTED_ACTION) && e.hasData(UIExAWTDataKey.CONTEXT_COMPONENT));
    }
}
