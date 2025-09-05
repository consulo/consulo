/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.ide.dnd.FileCopyPasteUtil;
import consulo.ide.impl.idea.openapi.editor.CustomFileDropHandler;
import consulo.project.Project;

import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchAction;
import consulo.versionControlSystem.impl.internal.patch.PatchFileType;
import jakarta.annotation.Nonnull;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

@ExtensionImpl
public class PatchFileDropHandler extends CustomFileDropHandler {

  @Override
  public boolean canHandle(@Nonnull Transferable t, Editor editor) {
    List<File> list = FileCopyPasteUtil.getFileList(t);
    if (list == null || list.size() != 1) return false;
    return PatchFileType.isPatchFile(list.get(0));
  }

  @Override
  public boolean handleDrop(@Nonnull Transferable t, Editor editor, @Nonnull Project project) {
    List<File> list = FileCopyPasteUtil.getFileList(t);
    if (list == null || list.size() != 1) return false;
    return ApplyPatchAction.showAndGetApplyPatch(project, list.get(0));
  }
}
