/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.fileChooser;

import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.PathChooserDialog;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.ui.app.WindowWrapper;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class UnifiedChooserDialog implements PathChooserDialog, FileChooserDialog {
    private static class DialogImpl extends WindowWrapper {

        private final Project myProject;
        private final FileChooserDescriptor myDescriptor;
        private final AsyncResult<VirtualFile[]> myResult;

        private Tree<FileElement> myTree;

        public DialogImpl(Project project, FileChooserDescriptor descriptor, AsyncResult<VirtualFile[]> result) {
            super("Select File");
            myProject = project;
            myDescriptor = descriptor;
            myResult = result;
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        protected Component createCenterComponent(Disposable uiDisposable) {
            setOKEnabled(false);

            myTree = FileTreeComponent.create(myProject, myDescriptor, uiDisposable);

            myTree.addSelectListener(node -> {
                TreeNode<FileElement> value = node.getValue();
                if (value == null) {
                    return;
                }

                VirtualFile file = value.getValue().getFile();
                setOKEnabled(myDescriptor.isFileSelectable(file));
            });

            return ScrollableLayout.create(myTree);
        }

        @Nullable
        @Override
        protected Size getDefaultSize() {
            return new Size(400, 400);
        }

        @RequiredUIAccess
        @Override
        public void doOKAction() {
            VirtualFile file = myTree.getSelectedNode().getValue().getFile();

            super.doOKAction();

            UIAccess.current().give(() -> myResult.setDone(new VirtualFile[]{file}));
        }

        @RequiredUIAccess
        @Override
        public void doCancelAction() {
            super.doCancelAction();

            UIAccess.current().give((Runnable) myResult::setRejected);
        }
    }

    private FileChooserDescriptor myDescriptor;

    @Nullable
    private Project myProject;

    public UnifiedChooserDialog(@Nullable Project project, @Nonnull FileChooserDescriptor descriptor) {
        myDescriptor = descriptor;
        myProject = project;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
        return chooseAsync(myProject, toSelect == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{toSelect});
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public AsyncResult<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, @Nonnull VirtualFile[] toSelect) {
        AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
        DialogImpl dialog = new DialogImpl((Project) project, myDescriptor, result);
        dialog.showAsync();
        return result;
    }
}
