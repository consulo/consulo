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
package consulo.fileChooser;

import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Configures and shows a file chooser. Created by {@link FileChooserFactory#newBuilder(FileChooserDescriptor)}.
 * <p>
 * A cancelled chooser completes the future exceptionally with {@link java.util.concurrent.CancellationException}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public interface FileChooserBuilder {
    FileChooserBuilder project(@Nullable ComponentManager project);

    FileChooserBuilder parent(@Nullable Component parent);

    FileChooserBuilder toSelect(@Nullable Path toSelect);

    /**
     * The last opened path wins over the {@link #toSelect(Path) path to select}. Without it the last opened path is
     * used only when there is no path to select.
     */
    FileChooserBuilder preferLastOverExplicit();

    /**
     * Shows the chooser for a descriptor which is not {@link FileChooserDescriptor#isChooseMultiple() multiple} and
     * answers the chosen path. The path may belong to another file system than the default one - an entry inside an
     * archive, or a file on a remote platform.
     */
    @RequiredUIAccess
    CompletableFuture<Path> showSingle();

    /**
     * Shows the chooser and answers the chosen paths - one path for a descriptor which is not
     * {@link FileChooserDescriptor#isChooseMultiple() multiple}.
     *
     * @see #showSingle()
     */
    @RequiredUIAccess
    CompletableFuture<List<Path>> showMultiple();

    /**
     * {@link #showSingle()} for the callers which still work on the virtual file system. The path is resolved off the
     * UI thread, the future completes on the UI thread - cancelled when the path has no virtual file.
     */
    @RequiredUIAccess
    CompletableFuture<VirtualFile> showSingleAsVirtualFile();

    /**
     * {@link #showMultiple()} for the callers which still work on the virtual file system. The paths are resolved off
     * the UI thread, the future completes on the UI thread. Paths without a virtual file are skipped, the future is
     * cancelled when none is left.
     */
    @RequiredUIAccess
    CompletableFuture<List<VirtualFile>> showMultipleAsVirtualFiles();
}
