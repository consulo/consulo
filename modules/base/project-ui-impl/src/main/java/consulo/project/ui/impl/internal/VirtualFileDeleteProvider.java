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
package consulo.project.ui.impl.internal;

import consulo.application.WriteAction;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public final class VirtualFileDeleteProvider implements DeleteProvider {
    private static final Logger LOG = Logger.getInstance(VirtualFileDeleteProvider.class);

    @Override
    public boolean canDeleteElement(@Nonnull DataContext dataContext) {
        final VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
        return files != null && files.length > 0;
    }

    @Override
    @RequiredUIAccess
    public void deleteElement(@Nonnull DataContext dataContext) {
        final VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
        if (files == null || files.length == 0) {
            return;
        }
        Project project = dataContext.getData(Project.KEY);

        String message = createConfirmationMessage(files).get();
        int returnValue = Messages.showOkCancelDialog(
            message,
            UILocalize.deleteDialogTitle().get(),
            ApplicationLocalize.buttonDelete().get(),
            CommonLocalize.buttonCancel().get(),
            UIUtil.getQuestionIcon()
        );
        if (returnValue != Messages.OK) {
            return;
        }

        Arrays.sort(files, FileComparator.getInstance());

        List<String> problems = new LinkedList<>();
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(ProjectLocalize.commandDeletingFiles())
            .run(() -> new Task.Modal(project, ProjectLocalize.progressTitleDeletingFiles(), true) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    int i = 0;
                    for (VirtualFile file : files) {
                        indicator.checkCanceled();
                        indicator.setText2Value(LocalizeValue.of(file.getPresentableUrl()));
                        indicator.setFraction((double)i / files.length);
                        i++;

                        try {
                            WriteAction.run(() -> file.delete(this));
                        }
                        catch (Exception e) {
                            LOG.info("Error when deleting " + file, e);
                            problems.add(file.getName());
                        }
                    }
                }

                @RequiredUIAccess
                @Override
                public void onSuccess() {
                    reportProblems();
                }

                @RequiredUIAccess
                @Override
                public void onCancel() {
                    reportProblems();
                }

                @RequiredUIAccess
                private void reportProblems() {
                    if (!problems.isEmpty()) {
                        reportDeletionProblem(problems);
                    }
                }
            }.queue());
    }

    @RequiredUIAccess
    private static void test() {
        reportDeletionProblem(List.of());
    }

    @RequiredUIAccess
    private static void reportDeletionProblem(List<String> problems) {
        boolean more = false;
        if (problems.size() > 10) {
            problems = problems.subList(0, 10);
            more = true;
        }
        Messages.showMessageDialog(
            "Could not erase files or folders:\n  " + StringUtil.join(problems, ",\n  ") + (more ? "\n  ..." : ""),
            UILocalize.errorDialogTitle().get(),
            UIUtil.getErrorIcon()
        );
    }

    private static final class FileComparator implements Comparator<VirtualFile> {
        private static final FileComparator ourInstance = new FileComparator();

        public static FileComparator getInstance() {
            return ourInstance;
        }

        @Override
        public int compare(final VirtualFile o1, final VirtualFile o2) {
            // files first
            return o2.getPath().compareTo(o1.getPath());
        }
    }

    private static LocalizeValue createConfirmationMessage(VirtualFile[] filesToDelete) {
        if (filesToDelete.length == 1) {
            if (filesToDelete[0].isDirectory()) {
                return UILocalize.areYouSureYouWantToDeleteSelectedFolderConfirmationMessage(filesToDelete[0].getName());
            }
            else {
                return UILocalize.areYouSureYouWantToDeleteSelectedFileConfirmationMessage(filesToDelete[0].getName());
            }
        }
        else {
            boolean hasFiles = false;
            boolean hasFolders = false;
            for (VirtualFile file : filesToDelete) {
                boolean isDirectory = file.isDirectory();
                hasFiles |= !isDirectory;
                hasFolders |= isDirectory;
            }
            LOG.assertTrue(hasFiles || hasFolders);
            if (hasFiles && hasFolders) {
                return UILocalize.areYouSureYouWantToDeleteSelectedFilesAndDirectoriesConfirmationMessage(filesToDelete.length);
            }
            else if (hasFolders) {
                return UILocalize.areYouSureYouWantToDeleteSelectedFoldersConfirmationMessage(filesToDelete.length);
            }
            else {
                return UILocalize.areYouSureYouWantToDeleteSelectedFilesAndFilesConfirmationMessage(filesToDelete.length);
            }
        }
    }
}