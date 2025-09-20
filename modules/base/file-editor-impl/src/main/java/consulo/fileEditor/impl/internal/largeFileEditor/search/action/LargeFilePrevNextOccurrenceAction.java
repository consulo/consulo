// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.dataContext.DataContext;
import consulo.fileEditor.impl.internal.largeFileEditor.search.CloseSearchTask;
import consulo.fileEditor.impl.internal.search.ContextAwareShortcutProvider;
import consulo.fileEditor.impl.internal.search.SearchUtils;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LargeFilePrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {

    private final LfeSearchManager mySearchManager;
    private final boolean myDirectionForward;

    public LargeFilePrevNextOccurrenceAction(LfeSearchManager searchManager, boolean directionForward) {
        mySearchManager = searchManager;
        myDirectionForward = directionForward;

        copyFrom(ActionManager.getInstance().getAction(
            directionForward ? IdeActions.ACTION_NEXT_OCCURENCE : IdeActions.ACTION_PREVIOUS_OCCURENCE));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (isEnabled()) {
            mySearchManager.gotoNextOccurrence(myDirectionForward);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled());
    }

    @Override
    @Nonnull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private boolean isEnabled() {
        CloseSearchTask task = (CloseSearchTask) mySearchManager.getLastExecutedCloseSearchTask();
        return task == null || task.isFinished();
    }

    @Override
    @Nullable
    public ShortcutSet getShortcut(@Nonnull DataContext context) {
        List<Shortcut> list = new ArrayList<>();
        boolean isSingleLine = !isMultiLine();
        if (myDirectionForward) {
            list.addAll(SearchUtils.shortcutsOf(IdeActions.ACTION_FIND_NEXT));
            if (isSingleLine) {
                list.addAll(SearchUtils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN));
                Collections.addAll(list, CommonShortcuts.ENTER.getShortcuts());
            }
        }
        else {
            list.addAll(SearchUtils.shortcutsOf(IdeActions.ACTION_FIND_PREVIOUS));
            if (isSingleLine) {
                list.addAll(SearchUtils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_UP));
                list.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null));
            }
        }
        return SearchUtils.shortcutSetOf(list);
    }

    private boolean isMultiLine() {
        SearchReplaceComponent searchReplaceComponent = mySearchManager.getSearchReplaceComponent();
        return searchReplaceComponent != null && searchReplaceComponent.isMultiline();
    }
}
