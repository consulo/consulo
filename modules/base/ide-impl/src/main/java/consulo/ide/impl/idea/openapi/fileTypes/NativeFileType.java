/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileTypes;

import consulo.process.cmd.GeneralCommandLine;
import consulo.application.AllIcons;
import consulo.application.util.SystemInfo;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.FileTypeLocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.INativeFileType;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class NativeFileType implements INativeFileType {
  public static final NativeFileType INSTANCE = new NativeFileType();

  private NativeFileType() { }

  @Override
  @Nonnull
  public String getId() {
    return "Native";
  }

  @Override
  @Nonnull
  public LocalizeValue getDescription() {
    return FileTypeLocalize.nativeFileTypeDescription();
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Custom;
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean openFileInAssociatedApplication(final ComponentManager project, @Nonnull final VirtualFile file) {
    return openAssociatedApplication(file);
  }

  public static boolean openAssociatedApplication(@Nonnull final VirtualFile file) {
    final List<String> commands = new ArrayList<>();
    if (SystemInfo.isWindows) {
      commands.add("rundll32.exe");
      commands.add("url.dll,FileProtocolHandler");
    }
    else if (SystemInfo.isMac) {
      commands.add("/usr/bin/open");
    }
    else if (SystemInfo.hasXdgOpen()) {
      commands.add("xdg-open");
    }
    else {
      return false;
    }
    commands.add(file.getPath());

    try {
      new GeneralCommandLine(commands).createProcess();
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }
}
