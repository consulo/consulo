/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeStyle;

import consulo.application.Result;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.LineSeparator;
import consulo.language.editor.WriteCommandAction;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * @author Nikolai Matveev
 */
public abstract class AbstractConvertLineSeparatorsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AbstractConvertLineSeparatorsAction.class);

    @Nonnull
    private final String mySeparator;

    protected AbstractConvertLineSeparatorsAction(@Nullable String text, @Nonnull LineSeparator separator) {
        this(LocalizeValue.localizeTODO(separator + " - " + text), separator.getSeparatorString());
    }

    protected AbstractConvertLineSeparatorsAction(@Nonnull LocalizeValue text, @Nonnull String separator) {
        super(text);
        mySeparator = separator;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        if (project != null) {
            final VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
            final Presentation presentation = e.getPresentation();
            if (virtualFiles != null) {
                if (virtualFiles.length == 1) {
                    presentation.setEnabled(!mySeparator.equals(LoadTextUtil.detectLineSeparator(virtualFiles[0], false)));
                }
                else {
                    presentation.setEnabled(true);
                }
            }
            else {
                presentation.setEnabled(false);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent event) {
        final Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }

        final VirtualFile[] virtualFiles = event.getData(VirtualFile.KEY_OF_ARRAY);
        if (virtualFiles == null) {
            return;
        }

        final VirtualFile projectVirtualDirectory;
        VirtualFile projectBaseDir = project.getBaseDir();
        if (projectBaseDir != null && projectBaseDir.isDirectory()) {
            projectVirtualDirectory = projectBaseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
        }
        else {
            projectVirtualDirectory = null;
        }

        final FileTypeRegistry fileTypeManager = FileTypeRegistry.getInstance();
        for (VirtualFile file : virtualFiles) {
            VfsUtilCore.processFilesRecursively(
                file,
                file1 -> {
                    if (shouldProcess(file1, project)) {
                        changeLineSeparators(project, file1, mySeparator);
                    }
                    return true;
                },
                dir -> {
                    return !dir.equals(projectVirtualDirectory) &&
                        !fileTypeManager.isFileIgnored(dir); // Exclude files like '.git'
                }
            );
        }
    }

    public static boolean shouldProcess(@Nonnull VirtualFile file, @Nonnull Project project) {
        if (file.isDirectory()
            || !file.isWritable()
            || FileTypeRegistry.getInstance().isFileIgnored(file)
            || file.getFileType().isBinary()
            || file.equals(project.getProjectFile())
            || file.equals(project.getWorkspaceFile())) {
            return false;
        }
        return true;
    }

    public static void changeLineSeparators(
        @Nonnull final Project project,
        @Nonnull final VirtualFile virtualFile,
        @Nonnull final String newSeparator
    ) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getCachedDocument(virtualFile);
        if (document != null) {
            fileDocumentManager.saveDocument(document);
        }

        String currentSeparator = LoadTextUtil.detectLineSeparator(virtualFile, false);
        final String commandText;
        if (StringUtil.isEmpty(currentSeparator)) {
            commandText = "Changed line separators to " + LineSeparator.fromString(newSeparator);
        }
        else {
            commandText = String.format(
                "Changed line separators from %s to %s",
                LineSeparator.fromString(currentSeparator),
                LineSeparator.fromString(newSeparator)
            );
        }

        new WriteCommandAction(project, commandText) {
            @Override
            protected void run(@Nonnull Result result) throws Throwable {
                try {
                    LoadTextUtil.changeLineSeparators(project, virtualFile, newSeparator, this);
                }
                catch (IOException e) {
                    LOG.info(e);
                }
            }
        }.execute();
    }
}
