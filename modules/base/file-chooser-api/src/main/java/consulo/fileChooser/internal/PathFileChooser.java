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
package consulo.fileChooser.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chooses on a remote machine, or on any machine when the descriptor allows it - places where no virtual file can
 * stand for a file, so the answer is paths.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PathFileChooser {
    @RequiredUIAccess
    CompletableFuture<List<Path>> choose(FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Path toSelect);
}
