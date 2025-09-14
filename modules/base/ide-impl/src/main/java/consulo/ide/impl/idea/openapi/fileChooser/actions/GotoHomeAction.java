/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author Vladimir Kondratyev
 */
@ActionImpl(id = "FileChooser.GotoHome")
public final class GotoHomeAction extends FileChooserAction {
    public GotoHomeAction() {
        super(
            ActionLocalize.actionFilechooserGotohomeText(),
            ActionLocalize.actionFilechooserGotohomeDescription(),
            PlatformIconGroup.nodesHomefolder()
        );
    }

    @Override
    protected void actionPerformed(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
        VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
        if (userHomeDir != null) {
            fileSystemTree.select(userHomeDir, () -> fileSystemTree.expand(userHomeDir, null));
        }
    }

    @Override
    protected void update(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }

        VirtualFile userHomeDir = VfsUtil.getUserHomeDir();
        presentation.setEnabled(userHomeDir != null && fileSystemTree.isUnderRoots(userHomeDir));
    }
}
