// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.fileEditor.impl.internal.search.StatusTextAction;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public final class LargeFileStatusTextAction extends StatusTextAction {
    private final LfeSearchManager searchManager;

    public LargeFileStatusTextAction(LfeSearchManager searchManager) {
        super();
        this.searchManager = searchManager;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        searchManager.updateStatusText();
        String statusText = searchManager.getStatusText();

        JLabel label = (JLabel) e.getPresentation().getClientProperty(COMPONENT_KEY);
        if (label != null) {
            label.setText(statusText);
            label.setVisible(StringUtil.isNotEmpty(statusText));
        }
    }
}
