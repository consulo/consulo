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
package com.intellij.ide.scratch;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * @author gregsh
 */
public class ScratchUtil {
  private ScratchUtil() {
  }

  /**
   * Returns true if a file or a directory is in one of scratch roots: scratch, console, etc.
   *
   * @see RootType
   * @see ScratchFileService
   */
  public static boolean isScratch(@Nullable VirtualFile file) {
    RootType rootType = RootType.forFile(file);
    return rootType != null && !rootType.isHidden();
  }

  public static void updateFileExtension(@Nonnull Project project, @Nullable VirtualFile file) throws IOException {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (CommandProcessor.getInstance().getCurrentCommand() == null) {
      throw new AssertionError("command required");
    }

    if (file == null) return;
    Language language = LanguageUtil.getLanguageForPsi(project, file);
    FileType expected = getFileTypeFromName(file);
    FileType actual = language == null ? null : language.getAssociatedFileType();
    if (expected == actual || actual == null) return;
    String ext = actual.getDefaultExtension();
    if (StringUtil.isEmpty(ext)) return;

    String newName = PathUtil.makeFileName(file.getNameWithoutExtension(), ext);
    VirtualFile parent = file.getParent();
    newName = parent != null && parent.findChild(newName) != null ? PathUtil.makeFileName(file.getName(), ext) : newName;
    file.rename(ScratchUtil.class, newName);
  }

  public static boolean hasMatchingExtension(@Nonnull Project project, @Nonnull VirtualFile file) {
    FileType expected = getFileTypeFromName(file);
    Language language = LanguageUtil.getLanguageForPsi(project, file);
    FileType actual = language == null ? null : language.getAssociatedFileType();
    return expected == actual && actual != null;
  }

  @Nullable
  public static FileType getFileTypeFromName(@Nonnull VirtualFile file) {
    String extension = file.getExtension();
    return extension == null ? null : FileTypeManager.getInstance().getFileTypeByExtension(extension);
  }

  @Nonnull
  public static String getRelativePath(@Nonnull Project project, @Nonnull VirtualFile file) {
    RootType rootType = Objects.requireNonNull(RootType.forFile(file));
    String rootPath = ScratchFileService.getInstance().getRootPath(rootType);
    VirtualFile rootFile = LocalFileSystem.getInstance().findFileByPath(rootPath);
    if (rootFile == null || !VfsUtilCore.isAncestor(rootFile, file, false)) {
      throw new AssertionError(file.getPath());
    }
    StringBuilder sb = new StringBuilder();
    for (VirtualFile o = file; !rootFile.equals(o); o = o.getParent()) {
      String part = StringUtil.notNullize(rootType.substituteName(project, o), o.getName());
      if (sb.length() == 0 && part.indexOf('/') > -1) {
        // db console root type adds folder here, trim it
        part = part.substring(part.lastIndexOf('/') + 1);
      }
      sb.insert(0, "/" + part);
    }
    sb.insert(0, rootType.getDisplayName());
    if (sb.charAt(sb.length() - 1) == ']') {
      // db console root type adds [data source name] here, trim it
      int idx = sb.lastIndexOf(" [");
      if (idx > 0 && sb.indexOf("/" + sb.substring(idx + 2, sb.length() - 1) + "/") < idx) {
        sb.setLength(idx);
      }
    }
    return sb.toString();
  }
}
