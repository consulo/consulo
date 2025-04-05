// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.awt.datatransfer.StringSelection;

import static consulo.ui.ex.action.ActionPlaces.KEYBOARD_SHORTCUT;

public class CopyPathsAction extends AnAction implements DumbAware {
    public CopyPathsAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (files != null && files.length > 0) {
            CopyPasteManager.getInstance().setContents(new StringSelection(getPaths(files)));
        }
    }

    private static String getPaths(VirtualFile[] files) {
        StringBuilder buf = new StringBuilder(files.length * 64);
        for (VirtualFile file : files) {
            if (buf.length() > 0) {
                buf.append('\n');
            }
            buf.append(file.getPresentableUrl());
        }
        return buf.toString();
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(KEYBOARD_SHORTCUT.equals(event.getPlace()));
    }
}
