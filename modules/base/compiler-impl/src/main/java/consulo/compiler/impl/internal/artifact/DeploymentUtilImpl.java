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
package consulo.compiler.impl.internal.artifact;

import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2013-06-11
 */
public class DeploymentUtilImpl {
    private static final Logger LOG = Logger.getInstance(DeploymentUtilImpl.class);

    public static void copyFile(
        @Nonnull File fromFile,
        @Nonnull File toFile,
        @Nonnull CompileContext context,
        @Nullable Set<String> writtenPaths,
        @Nullable FileFilter fileFilter
    ) throws IOException {
        if (fileFilter != null && !fileFilter.accept(fromFile)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping " + fromFile.getAbsolutePath() + ": it wasn't accepted by filter " + fileFilter);
            }
            return;
        }
        checkPathDoNotNavigatesUpFromFile(fromFile);
        checkPathDoNotNavigatesUpFromFile(toFile);
        if (fromFile.isDirectory()) {
            File[] fromFiles = fromFile.listFiles();
            toFile.mkdirs();
            for (File file : fromFiles) {
                copyFile(file, new File(toFile, file.getName()), context, writtenPaths, fileFilter);
            }
            return;
        }
        if (toFile.isDirectory()) {
            context.addMessage(
                CompilerMessageCategory.ERROR,
                CompilerLocalize.messageTextDestinationIsDirectory(createCopyErrorMessage(fromFile, toFile)).get(),
                null,
                -1,
                -1
            );
            return;
        }
        if (FileUtil.filesEqual(fromFile, toFile) || writtenPaths != null && !writtenPaths.add(toFile.getPath())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping " + fromFile.getAbsolutePath() + ": " + toFile.getAbsolutePath() + " is already written");
            }
            return;
        }
        if (!FileUtil.isFilePathAcceptable(toFile, fileFilter)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "Skipping " + fromFile.getAbsolutePath() + ": " + toFile.getAbsolutePath() +
                        " wasn't accepted by filter " + fileFilter
                );
            }
            return;
        }
        context.getProgressIndicator().setTextValue(LocalizeValue.localizeTODO("Copying files"));
        context.getProgressIndicator().setText2Value(LocalizeValue.of(fromFile.getPath()));
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Copy file '" + fromFile + "' to '" + toFile + "'");
            }
            if (toFile.exists() && !Platform.current().fs().isCaseSensitive()) {
                File canonicalFile = toFile.getCanonicalFile();
                if (!canonicalFile.getAbsolutePath().equals(toFile.getAbsolutePath())) {
                    FileUtil.delete(toFile);
                }
            }
            FileUtil.copy(fromFile, toFile, FilePermissionCopier.BY_NIO2);
        }
        catch (IOException e) {
            context.addMessage(
                CompilerMessageCategory.ERROR,
                createCopyErrorMessage(fromFile, toFile) + ": " + ExceptionUtil.getThrowableText(e),
                null,
                -1,
                -1
            );
        }
    }

    // OS X is sensitive for that
    private static void checkPathDoNotNavigatesUpFromFile(File file) {
        String path = file.getPath();
        int i = path.indexOf("..");
        if (i != -1) {
            String filepath = path.substring(0, i - 1);
            File filepart = new File(filepath);
            if (filepart.exists() && !filepart.isDirectory()) {
                LOG.error("Incorrect file path: '" + path + '\'');
            }
        }
    }

    private static String createCopyErrorMessage(File fromFile, File toFile) {
        return CompilerLocalize.messageTextErrorCopyingFileToFile(
            FileUtil.toSystemDependentName(fromFile.getPath()),
            FileUtil.toSystemDependentName(toFile.getPath())
        ).get();
    }
}
