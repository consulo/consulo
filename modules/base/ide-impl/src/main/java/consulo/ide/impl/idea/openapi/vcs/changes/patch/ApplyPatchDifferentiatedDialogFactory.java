/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ApplyPatchDifferentiatedDialogFactory {
  DialogWrapper create(final Project project,
                       final ApplyPatchExecutor callback,
                       final List<ApplyPatchExecutor> executors,
                       @Nonnull final ApplyPatchMode applyPatchMode,
                       @Nonnull final VirtualFile patchFile);

  FileChooserDescriptor createSelectPatchDescriptor();
}
