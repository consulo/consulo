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
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Configures and shows a save dialog. Created by {@link FileChooserFactory#newSaverBuilder(FileSaverDescriptor)}.
 * <p>
 * A cancelled dialog completes the future exceptionally with {@link java.util.concurrent.CancellationException}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public interface FileSaverBuilder {
    FileSaverBuilder project(@Nullable ComponentManager project);

    FileSaverBuilder parent(@Nullable Component parent);

    FileSaverBuilder baseDir(@Nullable Path baseDir);

    FileSaverBuilder fileName(@Nullable String fileName);

    /**
     * Shows the dialog and answers the path of the file to save - the file may not exist yet.
     */
    @RequiredUIAccess
    CompletableFuture<Path> showAsync();
}
