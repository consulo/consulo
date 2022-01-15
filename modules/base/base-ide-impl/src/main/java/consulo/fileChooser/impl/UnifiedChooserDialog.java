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
package consulo.fileChooser.impl;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.Tree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.app.WindowWrapper;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    protected Component createCenterComponent() {
      setOKEnabled(false);

      myTree = FileTreeComponent.create(myProject, myDescriptor);
      
      myTree.addSelectListener(node -> {
        VirtualFile file = node.getValue().getFile();
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

      UIAccess.current().give((Runnable)myResult::setRejected);
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
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable Project project, @Nonnull VirtualFile[] toSelect) {
    AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
    DialogImpl dialog = new DialogImpl(project, myDescriptor, result);
    dialog.showAsync();
    return result;
  }
}
