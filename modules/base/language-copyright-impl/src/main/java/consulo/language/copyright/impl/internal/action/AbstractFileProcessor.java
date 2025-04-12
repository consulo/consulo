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
package consulo.language.copyright.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.editor.FileModificationService;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractFileProcessor {
    private final Project myProject;
    private final Module myModule;
    private PsiDirectory directory = null;
    private PsiFile file = null;
    private PsiFile[] files = null;
    private boolean subdirs = false;
    private final String message;
    private final String title;

    protected abstract Runnable preprocessFile(PsiFile psifile) throws IncorrectOperationException;

    protected AbstractFileProcessor(Project project, String title, String message) {
        myProject = project;
        myModule = null;
        directory = null;
        subdirs = true;
        this.title = title;
        this.message = message;
    }

    protected AbstractFileProcessor(Project project, Module module, String title, String message) {
        myProject = project;
        myModule = module;
        directory = null;
        subdirs = true;
        this.title = title;
        this.message = message;
    }

    protected AbstractFileProcessor(Project project, PsiDirectory dir, boolean subdirs, String title, String message) {
        myProject = project;
        myModule = null;
        directory = dir;
        this.subdirs = subdirs;
        this.message = message;
        this.title = title;
    }

    protected AbstractFileProcessor(Project project, PsiFile file, String title, String message) {
        myProject = project;
        myModule = null;
        this.file = file;
        this.message = message;
        this.title = title;
    }

    protected AbstractFileProcessor(Project project, PsiFile[] files, String title, String message, Runnable runnable) {
        myProject = project;
        myModule = null;
        this.files = files;
        this.message = message;
        this.title = title;
    }

    @RequiredUIAccess
    public void run() {
        if (directory != null) {
            process(directory, subdirs);
        }
        else if (files != null) {
            process(files);
        }
        else if (file != null) {
            process(file);
        }
        else if (myModule != null) {
            process(myModule);
        }
        else if (myProject != null) {
            process(myProject);
        }
    }

    @RequiredUIAccess
    private void process(final PsiFile file) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) {
            return;
        }
        final Runnable[] resultRunnable = new Runnable[1];

        execute(
            () -> {
                try {
                    resultRunnable[0] = preprocessFile(file);
                }
                catch (IncorrectOperationException incorrectoperationexception) {
                    logger.error(incorrectoperationexception);
                }
            },
            () -> {
                if (resultRunnable[0] != null) {
                    resultRunnable[0].run();
                }
            }
        );
    }

    private Runnable prepareFiles(List<PsiFile> files) {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        String msg = null;
        double fraction = 0.0D;
        if (indicator != null) {
            msg = indicator.getText();
            fraction = indicator.getFraction();
            indicator.setText(message);
        }

        final Runnable[] runnables = new Runnable[files.size()];
        for (int i = 0; i < files.size(); i++) {
            PsiFile pfile = files.get(i);
            if (pfile == null) {
                logger.debug("Unexpected null file at " + i);
                continue;
            }
            if (indicator != null) {
                if (indicator.isCanceled()) {
                    return null;
                }

                indicator.setFraction((double)i / (double)files.size());
            }

            if (pfile.isWritable()) {
                try {
                    runnables[i] = preprocessFile(pfile);
                }
                catch (IncorrectOperationException incorrectoperationexception) {
                    logger.error(incorrectoperationexception);
                }
            }

            files.set(i, null);
        }

        if (indicator != null) {
            indicator.setText(msg);
            indicator.setFraction(fraction);
        }

        return () -> {
            ProgressIndicator indicator1 = ProgressManager.getInstance().getProgressIndicator();
            String msg1 = null;
            double fraction1 = 0.0D;
            if (indicator1 != null) {
                msg1 = indicator1.getText();
                fraction1 = indicator1.getFraction();
                indicator1.setText(message);
            }

            for (int j = 0; j < runnables.length; j++) {
                if (indicator1 != null) {
                    if (indicator1.isCanceled()) {
                        return;
                    }

                    indicator1.setFraction((double)j / (double)runnables.length);
                }

                Runnable runnable = runnables[j];
                if (runnable != null) {
                    runnable.run();
                }
                runnables[j] = null;
            }

            if (indicator1 != null) {
                indicator1.setText(msg1);
                indicator1.setFraction(fraction1);
            }
        };
    }

    @RequiredUIAccess
    private void process(final PsiFile[] files) {
        final Runnable[] resultRunnable = new Runnable[1];
        execute(
            () -> resultRunnable[0] = prepareFiles(new ArrayList<>(Arrays.asList(files))),
            () -> {
                if (resultRunnable[0] != null) {
                    resultRunnable[0].run();
                }
            }
        );
    }

    @RequiredUIAccess
    private void process(final PsiDirectory dir, final boolean subdirs) {
        final List<PsiFile> pfiles = new ArrayList<>();
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(() -> findFiles(pfiles, dir, subdirs), title, true, myProject);
        handleFiles(pfiles);
    }

    @RequiredUIAccess
    private void process(final Project project) {
        final List<PsiFile> pfiles = new ArrayList<>();
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(() -> findFiles(project, pfiles), title, true, project);
        handleFiles(pfiles);
    }

    @RequiredUIAccess
    private void process(final Module module) {
        final List<PsiFile> pfiles = new ArrayList<>();
        ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(() -> findFiles(module, pfiles), title, true, myProject);
        handleFiles(pfiles);
    }

    @RequiredReadAction
    private static void findFiles(Project project, List<PsiFile> files) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            findFiles(module, files);
        }
    }

    protected static void findFiles(final Module module, final List<PsiFile> files) {
        final ModuleFileIndex idx = ModuleRootManager.getInstance(module).getFileIndex();

        final VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();

        for (VirtualFile root : roots) {
            idx.iterateContentUnderDirectory(
                root,
                dir -> {
                    if (dir.isDirectory()) {
                        final PsiDirectory psiDir = PsiManager.getInstance(module.getProject()).findDirectory(dir);
                        if (psiDir != null) {
                            findFiles(files, psiDir, false);
                        }
                    }
                    return true;
                }
            );
        }
    }

    @RequiredUIAccess
    private void handleFiles(final List<PsiFile> files) {
        final List<VirtualFile> vFiles = new ArrayList<>();
        for (PsiFile psiFile : files) {
            vFiles.add(psiFile.getVirtualFile());
        }
        if (!ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(VirtualFileUtil.toVirtualFileArray(vFiles)).hasReadonlyFiles()
            && !files.isEmpty()) {
            final Runnable[] resultRunnable = new Runnable[1];
            execute(
                () -> resultRunnable[0] = prepareFiles(files),
                () -> {
                    if (resultRunnable[0] != null) {
                        resultRunnable[0].run();
                    }
                }
            );
        }
    }

    @RequiredReadAction
    private static void findFiles(List<PsiFile> files, PsiDirectory directory, boolean subdirs) {
        final Project project = directory.getProject();
        PsiFile[] locals = directory.getFiles();
        for (PsiFile local : locals) {
            CopyrightProfile opts = CopyrightManager.getInstance(project).getCopyrightOptions(local);
            if (opts != null && UpdateCopyrightsProvider.hasExtension(local)) {
                files.add(local);
            }
        }

        if (subdirs) {
            PsiDirectory[] dirs = directory.getSubdirectories();
            for (PsiDirectory dir : dirs) {
                findFiles(files, dir, subdirs);
            }
        }
    }

    @RequiredUIAccess
    private void execute(final Runnable readAction, final Runnable writeAction) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            readAction::run,
            title,
            true,
            myProject
        );
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(LocalizeValue.ofNullable(title))
            .inWriteAction()
            .run(writeAction);
    }

    private static final Logger logger = Logger.getInstance(AbstractFileProcessor.class);
}
