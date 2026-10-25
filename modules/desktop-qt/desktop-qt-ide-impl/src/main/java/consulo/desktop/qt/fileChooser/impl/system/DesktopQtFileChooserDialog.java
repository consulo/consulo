/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.fileChooser.impl.system;

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.widgets.QFileDialog;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFileChooserDialog {
    private final FileChooserDescriptor myDescriptor;

    public DesktopQtFileChooserDialog(FileChooserDescriptor descriptor) {
        myDescriptor = descriptor;
    }

    @RequiredUIAccess
    public CompletableFuture<List<Path>> chooseAsync(@Nullable Path toSelect) {
        Window focusedWindow = Window.getActiveWindow();

        QWidget parent = focusedWindow == null ? null : TargetQt.to(focusedWindow);

        Path directory = toSelect == null || Files.isDirectory(toSelect) ? toSelect : toSelect.getParent();

        CompletableFuture<List<Path>> result = new CompletableFuture<>();
        UIAccess.current().give(() -> {
            String path = QFileDialog.getExistingDirectory(parent, myDescriptor.getTitle().get(), directory == null ? "" : directory.toString());
            if (path != null && !path.isEmpty()) {
                result.complete(List.of(Path.of(path)));
            }
            else {
                result.completeExceptionally(new CancellationException());
            }
        });
        return result;
    }
}
