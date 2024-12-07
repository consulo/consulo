// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class ShowBreakpointsOverLineNumbersAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return isSelected();
    }

    public static boolean isSelected() {
        return UISettings.getInstance().getShowBreakpointsOverLineNumbers() && PersistentEditorSettings.getInstance().isLineNumbersShown();
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        UISettings.getInstance().setShowBreakpointsOverLineNumbers(state);
        EditorFactory.getInstance().refreshAllEditors();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(true);
        if (!PersistentEditorSettings.getInstance().isLineNumbersShown()) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
