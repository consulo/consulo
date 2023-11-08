/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.execution.ui.console.ConsoleRootType;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.scratch.FileEditorTrackingRootType;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * @author gregsh
 */
@ExtensionImpl
public class IdeConsoleRootType extends ConsoleRootType implements FileEditorTrackingRootType {
  @Inject
  public IdeConsoleRootType() {
    super("ide", "Consoles");
  }

  @Nonnull
  public static IdeConsoleRootType getInstance() {
    return findByClass(IdeConsoleRootType.class);
  }

  @Nullable
  @Override
  public Image substituteIcon(@Nonnull Project project, @Nonnull VirtualFile file) {
    if (file.isDirectory()) return null;
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence());
    Image icon =
      fileType == UnknownFileType.INSTANCE || fileType == PlainTextFileType.INSTANCE ? AllIcons.Debugger.Console : ObjectUtil.notNull(
        fileType.getIcon(),
        AllIcons.Debugger.Console);
    return ImageEffects.layered(icon, AllIcons.Nodes.RunnableMark);
  }

  @Override
  public void fileOpened(@Nonnull VirtualFile file, @Nonnull FileEditorManager source) {
    RunIdeConsoleAction.configureConsole(file, source);
  }
}
