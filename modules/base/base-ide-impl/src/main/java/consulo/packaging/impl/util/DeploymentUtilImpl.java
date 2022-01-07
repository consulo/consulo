/*
 * Copyright 2013-2016 consulo.io
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
package consulo.packaging.impl.util;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.deployment.DeploymentUtil;
import consulo.logging.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ExceptionUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Set;

/**
 * @author VISTALL
 * @since 11:43/11.06.13
 */
public class DeploymentUtilImpl extends DeploymentUtil {
  private static final Logger LOGGER = Logger.getInstance(DeploymentUtilImpl.class);

  public static void copyFile(@Nonnull final File fromFile,
                       @Nonnull final File toFile,
                       @Nonnull CompileContext context,
                       @Nullable Set<String> writtenPaths,
                       @Nullable FileFilter fileFilter) throws IOException {
    if (fileFilter != null && !fileFilter.accept(fromFile)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping " + fromFile.getAbsolutePath() + ": it wasn't accepted by filter " + fileFilter);
      }
      return;
    }
    checkPathDoNotNavigatesUpFromFile(fromFile);
    checkPathDoNotNavigatesUpFromFile(toFile);
    if (fromFile.isDirectory()) {
      final File[] fromFiles = fromFile.listFiles();
      toFile.mkdirs();
      for (File file : fromFiles) {
        copyFile(file, new File(toFile, file.getName()), context, writtenPaths, fileFilter);
      }
      return;
    }
    if (toFile.isDirectory()) {
      context.addMessage(CompilerMessageCategory.ERROR,
                         CompilerBundle.message("message.text.destination.is.directory", createCopyErrorMessage(fromFile, toFile)), null, -1, -1);
      return;
    }
    if (FileUtil.filesEqual(fromFile, toFile) || writtenPaths != null && !writtenPaths.add(toFile.getPath())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping " + fromFile.getAbsolutePath() + ": " + toFile.getAbsolutePath() + " is already written");
      }
      return;
    }
    if (!FileUtil.isFilePathAcceptable(toFile, fileFilter)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping " + fromFile.getAbsolutePath() + ": " + toFile.getAbsolutePath() + " wasn't accepted by filter " + fileFilter);
      }
      return;
    }
    context.getProgressIndicator().setText("Copying files");
    context.getProgressIndicator().setText2(fromFile.getPath());
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Copy file '" + fromFile + "' to '"+toFile+"'");
      }
      if (toFile.exists() && !SystemInfo.isFileSystemCaseSensitive) {
        File canonicalFile = toFile.getCanonicalFile();
        if (!canonicalFile.getAbsolutePath().equals(toFile.getAbsolutePath())) {
          FileUtil.delete(toFile);
        }
      }
      FileUtil.copy(fromFile, toFile);
    }
    catch (IOException e) {
      context.addMessage(CompilerMessageCategory.ERROR, createCopyErrorMessage(fromFile, toFile) + ": "+ ExceptionUtil.getThrowableText(e), null, -1, -1);
    }
  }

  // OS X is sensitive for that
  private static void checkPathDoNotNavigatesUpFromFile(File file) {
    String path = file.getPath();
    int i = path.indexOf("..");
    if (i != -1) {
      String filepath = path.substring(0,i-1);
      File filepart = new File(filepath);
      if (filepart.exists() && !filepart.isDirectory()) {
        LOGGER.error("Incorrect file path: '" + path + '\'');
      }
    }
  }

  private static String createCopyErrorMessage(final File fromFile, final File toFile) {
    return CompilerBundle.message("message.text.error.copying.file.to.file", FileUtil.toSystemDependentName(fromFile.getPath()),
                                  FileUtil.toSystemDependentName(toFile.getPath()));
  }
}
