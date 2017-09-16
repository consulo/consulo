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
package consulo.fileChooser;

import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import consulo.ui.Button;
import consulo.ui.Components;
import consulo.ui.DockLayout;
import consulo.ui.HorizontalLayout;
import consulo.ui.Layouts;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.Windows;
import consulo.ui.border.BorderPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebPathChooserDialog implements PathChooserDialog {
  @Override
  @RequiredUIAccess
  public void choose(@Nullable VirtualFile toSelect, @NotNull Consumer<List<VirtualFile>> callback) {
    Window fileTree = Windows.modalWindow("Select file");
    fileTree.setSize(new Size(400, 400));
    fileTree.setContent(Components.label("TEst"));

    DockLayout dockLayout = Layouts.dock();
    dockLayout.center(FileTreeComponent.create());

    DockLayout bottomLayout = Layouts.dock();
    bottomLayout.addBorder(BorderPosition.TOP);
    HorizontalLayout rightButtons = Layouts.horizontal();
    bottomLayout.right(rightButtons);

    Button ok = Components.button("OK");
    ok.setEnabled(false);
    rightButtons.add(ok);
    consulo.ui.Button cancel = Components.button("Cancel");
    cancel.addListener(Button.ClickHandler.class, () -> {
      fileTree.close();
    });

    rightButtons.add(cancel);
    dockLayout.bottom(bottomLayout);

    fileTree.setContent(dockLayout);

    fileTree.show();
  }
}
