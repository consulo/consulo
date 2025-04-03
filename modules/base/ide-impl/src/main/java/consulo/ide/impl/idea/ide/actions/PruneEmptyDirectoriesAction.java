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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.file.FileTypeManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;

import java.io.IOException;

public class PruneEmptyDirectoriesAction extends AnAction {
    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        e.getPresentation().setEnabled(files != null && files.length > 0);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        assert files != null;

        FileTypeManager ftManager = FileTypeManager.getInstance();
        try {
            for (VirtualFile file : files) {
                pruneEmptiesIn(file, ftManager);
            }
        }
        catch (IOException ignored) {
        }
    }

    private static void pruneEmptiesIn(VirtualFile file, final FileTypeManager ftManager) throws IOException {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
            @Override
            @RequiredUIAccess
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    if (ftManager.isFileIgnored(file)) {
                        return false;
                    }
                }
                else if (".DS_Store".equals(file.getName())) {
                    delete(file);
                    return false;
                }
                return true;
            }

            @Override
            @RequiredUIAccess
            public void afterChildrenVisited(@Nonnull VirtualFile file) {
                if (file.isDirectory() && file.getChildren().length == 0) {
                    delete(file);
                }
            }
        });
    }

    @RequiredUIAccess
    private static void delete(final VirtualFile file) {
        Application.get().runWriteAction(() -> {
            try {
                file.delete(PruneEmptyDirectoriesAction.class);
                //noinspection UseOfSystemOutOrSystemErr
                System.out.println("Deleted: " + file.getPresentableUrl());
            }
            catch (IOException e) {
                Messages.showErrorDialog("Cannot delete '" + file.getPresentableUrl() + "', " + e.getLocalizedMessage(), "IOException");
            }
        });
    }
}
