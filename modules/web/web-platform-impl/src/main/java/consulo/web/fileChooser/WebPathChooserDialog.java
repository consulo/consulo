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
package consulo.web.fileChooser;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import consulo.ui.*;
import consulo.ui.border.BorderPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebPathChooserDialog implements PathChooserDialog {
  private FileChooserDescriptor myDescriptor;
  @Nullable
  private Project myProject;

  public WebPathChooserDialog(@Nullable Project project, @NotNull FileChooserDescriptor descriptor) {
    myDescriptor = descriptor;
    myProject = project;
  }

  @Override
  @RequiredUIAccess
  public void choose(@Nullable VirtualFile toSelect, @NotNull Consumer<List<VirtualFile>> callback) {
    Window fileTree = Window.createModal("Select file");
    fileTree.setSize(new Size(400, 400));
    fileTree.setContent(Label.create("TEst"));

    DockLayout dockLayout = DockLayout.create();
    Tree<FileElement> component = FileTreeComponent.create(myProject, myDescriptor);

    dockLayout.center(component);

    DockLayout bottomLayout = DockLayout.create();
    bottomLayout.addBorder(BorderPosition.TOP);
    HorizontalLayout rightButtons = HorizontalLayout.create();
    bottomLayout.right(rightButtons);

    Button ok = Button.create("OK");
    ok.addClickListener(() -> {
      fileTree.close();

      VirtualFile file = component.getSelectedNode().getValue().getFile();
      
      UIAccess.get().give(() -> callback.consume(Arrays.asList(file)));
    });
    ok.setEnabled(false);
    rightButtons.add(ok);
    consulo.ui.Button cancel = Button.create("Cancel");
    cancel.addClickListener(fileTree::close);

    component.addSelectListener(node -> {
      VirtualFile file = node.getValue().getFile();
      ok.setEnabled(myDescriptor.isFileSelectable(file));
    });

    rightButtons.add(cancel);
    dockLayout.bottom(bottomLayout);

    fileTree.setContent(dockLayout);

    fileTree.show();
  }
}
