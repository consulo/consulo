/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.os.mac.internal.fileChooser;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.desktop.awt.ui.OwnerOptional;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.util.FileChooserUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.CommandProcessorEx;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MacPathChooserDialog {
    private FileDialog myFileDialog;
    private final FileChooserDescriptor myFileChooserDescriptor;
    private final WeakReference<Component> myParent;
    private final @Nullable Project myProject;
    private final LocalizeValue myTitle;

    public MacPathChooserDialog(FileChooserDescriptor descriptor, @Nullable Component parent, @Nullable Project project) {
        myFileChooserDescriptor = descriptor;
        myParent = new WeakReference<>(parent);
        myProject = project;
        myTitle = descriptor.getTitle().orIfEmpty(UILocalize.fileChooserDefaultTitle());

        Consumer<Dialog> dialogConsumer = owner -> myFileDialog = new FileDialog(owner, myTitle.get(), FileDialog.LOAD);
        Consumer<Frame> frameConsumer = owner -> myFileDialog = new FileDialog(owner, myTitle.get(), FileDialog.LOAD);

        OwnerOptional.fromComponent(parent).ifDialog(dialogConsumer).ifFrame(frameConsumer).ifNull(frameConsumer);
    }

    @RequiredUIAccess
    public CompletableFuture<List<Path>> chooseAsync(@Nullable Path toSelect) {
        if (toSelect != null && toSelect.getParent() != null) {
            if (Files.isDirectory(toSelect)) {
                myFileDialog.setDirectory(toSelect.toString());
                myFileDialog.setFile(null);
            }
            else {
                myFileDialog.setDirectory(toSelect.getParent().toString());
                myFileDialog.setFile(toSelect.toString());
            }
        }

        myFileDialog.setFilenameFilter((dir, name) -> FileChooserUtil.isPathAccepted(myFileChooserDescriptor, dir.toPath().resolve(name)));

        myFileDialog.setMultipleMode(myFileChooserDescriptor.isChooseMultiple());

        CompletableFuture<List<Path>> result = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            CommandProcessorEx commandProcessor =
                ApplicationManager.getApplication() != null ? (CommandProcessorEx)CommandProcessor.getInstance() : null;
            boolean appStarted = commandProcessor != null;

            if (appStarted) {
                commandProcessor.enterModal();
                LaterInvocator.enterModal(myFileDialog);
            }

            Component parent = myParent.get();
            try {
                myFileDialog.setVisible(true);
            }
            finally {
                if (appStarted) {
                    commandProcessor.leaveModal();
                    LaterInvocator.leaveModal(myFileDialog);
                    if (parent != null) {
                        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
                            () -> IdeFocusManager.getGlobalInstance().requestFocus(parent, true)
                        );
                    }
                }
            }

            List<Path> paths = new ArrayList<>();
            for (File file : myFileDialog.getFiles()) {
                paths.add(file.toPath().toAbsolutePath());
            }

            if (paths.isEmpty()) {
                result.completeExceptionally(new CancellationException());
                return;
            }

            FileChooserUtil.validateSelectedPaths(myFileChooserDescriptor, paths).whenComplete((message, error) -> {
                if (error != null) {
                    result.completeExceptionally(error);
                    return;
                }

                if (message.isNotEmpty()) {
                    if (parent == null) {
                        Messages.showErrorDialog(myProject, message.get(), myTitle.get());
                    }
                    else {
                        Messages.showErrorDialog(parent, message.get(), myTitle.get());
                    }

                    result.completeExceptionally(new CancellationException(message.get()));
                    return;
                }

                result.complete(paths);
            });
        });
        return result;
    }
}
