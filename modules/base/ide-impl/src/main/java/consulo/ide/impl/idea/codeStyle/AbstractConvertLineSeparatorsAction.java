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

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.LineSeparator;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import jakarta.annotation.Nonnull;

import java.io.IOException;

/**
 * @author Nikolai Matveev
 */
public abstract class AbstractConvertLineSeparatorsAction extends ToggleAction {
    private static final Logger LOG = Logger.getInstance(AbstractConvertLineSeparatorsAction.class);

    @Nonnull
    private final String mySeparator;

    protected AbstractConvertLineSeparatorsAction(@Nonnull LocalizeValue text, @Nonnull LineSeparator separator) {
        this(text, separator.getSeparatorString());
    }

    protected AbstractConvertLineSeparatorsAction(@Nonnull LocalizeValue text, @Nonnull String separator) {
        super(text);
        mySeparator = separator;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return false;
        }

        VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
        return virtualFiles != null && virtualFiles.length == 1
            && mySeparator.equals(LoadTextUtil.detectLineSeparator(virtualFiles[0], false));
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        if (!state) {
            return;
        }

        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (virtualFiles == null) {
            return;
        }

        VirtualFile projectBaseDir = project.getBaseDir();
        VirtualFile projectVirtualDirectory =
            projectBaseDir != null && projectBaseDir.isDirectory() ? projectBaseDir.findChild(Project.DIRECTORY_STORE_FOLDER) : null;

        FileTypeRegistry fileTypeManager = FileTypeRegistry.getInstance();
        for (VirtualFile file : virtualFiles) {
            VfsUtilCore.processFilesRecursively(
                file,
                file1 -> {
                    if (shouldProcess(file1, project)) {
                        changeLineSeparators(project, file1, mySeparator);
                    }
                    return true;
                },
                dir -> !dir.equals(projectVirtualDirectory) && !fileTypeManager.isFileIgnored(dir) // Exclude files like '.git'
            );
        }
    }

    public static boolean shouldProcess(@Nonnull VirtualFile file, @Nonnull Project project) {
        return !file.isDirectory()
            && file.isWritable()
            && !FileTypeRegistry.getInstance().isFileIgnored(file)
            && !file.getFileType().isBinary()
            && !file.equals(project.getProjectFile())
            && !file.equals(project.getWorkspaceFile());
    }

    @RequiredUIAccess
    public static void changeLineSeparators(@Nonnull Project project, @Nonnull VirtualFile virtualFile, @Nonnull String newSeparator) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getCachedDocument(virtualFile);
        if (document != null) {
            fileDocumentManager.saveDocument(document);
        }

        String currentSeparator = LoadTextUtil.detectLineSeparator(virtualFile, false);
        String commandText;
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

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(LocalizeValue.ofNullable(commandText))
            .inWriteAction()
            .run(() -> {
                try {
                    LoadTextUtil.changeLineSeparators(project, virtualFile, newSeparator, AbstractConvertLineSeparatorsAction.class);
                }
                catch (IOException e) {
                    LOG.info(e);
                }
            });
    }
}
