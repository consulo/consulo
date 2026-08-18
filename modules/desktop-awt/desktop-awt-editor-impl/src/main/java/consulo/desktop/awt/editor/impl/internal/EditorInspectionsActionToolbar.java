// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.internal;

import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.impl.action.toolbar.AdvancedActionToolbarImpl;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.keymap.KeymapManager;

import java.awt.LayoutManager;

public class EditorInspectionsActionToolbar extends AdvancedActionToolbarImpl {
    private final DesktopEditorImpl editor;

    public EditorInspectionsActionToolbar(ActionGroup actions, DesktopEditorImpl editor) {
        super(ActionPlaces.EDITOR_INSPECTIONS_TOOLBAR,
            actions,
            ActionToolbar.Style.HORIZONTAL,
            ActionManager.getInstance(),
            DataManager.getInstance(),
            Application.get(),
            KeymapManager.getInstance());
        this.editor = editor;
        setLayout(new DesktopEditorAnalyzeStatusPanel.StatusComponentLayout());
    }

    @Override
    public void addNotify() {
        setTargetComponent(editor.getContentComponent());
        super.addNotify();
    }

    @Override
    public void doLayout() {
        LayoutManager layoutManager = getLayout();
        if (layoutManager != null) {
            layoutManager.layoutContainer(this);
        }
        else {
            super.doLayout();
        }
    }
}
