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
package consulo.compiler.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.AsyncFileService;
import consulo.compiler.CompileContext;
import consulo.compiler.localize.CompilerLocalize;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * @author Jeka
 * @since 2002-01-03
 */
public class CompilerUtil {
    private static final Logger LOG = Logger.getInstance(CompilerUtil.class);

    public static String quotePath(String path) {
        if (path != null && path.indexOf(' ') != -1) {
            path = path.replaceAll("\\\\", "\\\\\\\\");
            path = '"' + path + '"';
        }
        return path;
    }

    public static void collectFiles(Collection<File> container, File rootDir, FileFilter fileFilter) {
        File[] files = rootDir.listFiles(fileFilter);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectFiles(container, file, fileFilter);
            }
            else {
                container.add(file);
            }
        }
    }

    public static Map<Module, List<VirtualFile>> buildModuleToFilesMap(CompileContext context, VirtualFile[] files) {
        return buildModuleToFilesMap(context, Arrays.asList(files));
    }


    public static Map<Module, List<VirtualFile>> buildModuleToFilesMap(CompileContext context, List<VirtualFile> files) {
        //assertion: all files are different
        Map<Module, List<VirtualFile>> map = new HashMap<>();
        Application.get().runReadAction(() -> {
            for (VirtualFile file : files) {
                Module module = context.getModuleByFile(file);

                if (module == null) {
                    continue; // looks like file invalidated
                }

                List<VirtualFile> moduleFiles = map.get(module);
                if (moduleFiles == null) {
                    moduleFiles = new ArrayList<>();
                    map.put(module, moduleFiles);
                }
                moduleFiles.add(file);
            }
        });
        return map;
    }

    /**
     * must not be called inside ReadAction
     */
    public static void refreshIOFiles(@Nonnull Collection<File> files) {
        if (!files.isEmpty()) {
            LocalFileSystem.getInstance().refreshIoFiles(files);
        }
    }

    public static void refreshIODirectories(@Nonnull Collection<File> files) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        List<VirtualFile> filesToRefresh = new ArrayList<>();
        for (File file : files) {
            VirtualFile virtualFile = lfs.refreshAndFindFileByIoFile(file);
            if (virtualFile != null) {
                filesToRefresh.add(virtualFile);
            }
        }
        if (!filesToRefresh.isEmpty()) {
            RefreshQueue.getInstance().refresh(false, true, null, filesToRefresh);
        }
    }

    public static void refreshIOFile(File file) {
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (vFile != null) {
            vFile.refresh(false, false);
        }
    }

    public static void addLocaleOptions(List<String> commandLine, boolean launcherUsed) {
        // need to specify default encoding so that javac outputs messages in 'correct' language
        //noinspection HardCodedStringLiteral
        commandLine.add((launcherUsed ? "-J" : "") + "-D" + CharsetToolkit.FILE_ENCODING_PROPERTY + "=" + CharsetToolkit.getDefaultSystemCharset()
            .name());
        // javac's VM should use the same default locale that IDEA uses in order for javac to print messages in 'correct' language
        //noinspection HardCodedStringLiteral
        String lang = System.getProperty("user.language");
        if (lang != null) {
            //noinspection HardCodedStringLiteral
            commandLine.add((launcherUsed ? "-J" : "") + "-Duser.language=" + lang);
        }
        //noinspection HardCodedStringLiteral
        String country = System.getProperty("user.country");
        if (country != null) {
            //noinspection HardCodedStringLiteral
            commandLine.add((launcherUsed ? "-J" : "") + "-Duser.country=" + country);
        }
        //noinspection HardCodedStringLiteral
        String region = System.getProperty("user.region");
        if (region != null) {
            //noinspection HardCodedStringLiteral
            commandLine.add((launcherUsed ? "-J" : "") + "-Duser.region=" + region);
        }
    }

    public static <T extends Throwable> void runInContext(CompileContext context, String title, ThrowableRunnable<T> action) throws T {
        if (title != null) {
            context.getProgressIndicator().pushState();
            context.getProgressIndicator().setText(title);
        }
        try {
            action.run();
        }
        finally {
            if (title != null) {
                context.getProgressIndicator().popState();
            }
        }
    }

    public static void logDuration(String activityName, long duration) {
        LOG.info(activityName + " took " + duration + " ms: " + duration / 60000 + " min " + (duration % 60000) / 1000 + "sec");
    }

    public static void clearOutputDirectories(Collection<File> outputDirectories) {
        long start = System.currentTimeMillis();
        // do not delete directories themselves, or we'll get rootsChanged() otherwise
        Collection<File> filesToDelete = new ArrayList<>(outputDirectories.size() * 2);
        for (File outputDirectory : outputDirectories) {
            File[] files = outputDirectory.listFiles();
            if (files != null) {
                ContainerUtil.addAll(filesToDelete, files);
            }
        }
        if (filesToDelete.size() > 0) {
            Application.get().getInstance(AsyncFileService.class).asyncDelete(filesToDelete);

            // ensure output directories exist
            for (File file : outputDirectories) {
                file.mkdirs();
            }
            long clearStop = System.currentTimeMillis();

            refreshIODirectories(outputDirectories);

            long refreshStop = System.currentTimeMillis();

            logDuration("Clearing output dirs", clearStop - start);
            logDuration("Refreshing output directories", refreshStop - clearStop);
        }
    }

    @RequiredReadAction
    public static void computeIntersectingPaths(Project project, Collection<VirtualFile> outputPaths, Collection<VirtualFile> result) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            VirtualFile[] sourceRoots = rootManager.getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
            for (VirtualFile outputPath : outputPaths) {
                for (VirtualFile sourceRoot : sourceRoots) {
                    if (VirtualFileUtil.isAncestor(outputPath, sourceRoot, true)
                        || VirtualFileUtil.isAncestor(sourceRoot, outputPath, false)) {
                        result.add(outputPath);
                    }
                }
            }
        }
    }

    @RequiredUIAccess
    public static boolean askUserToContinueWithNoClearing(Project project, Collection<VirtualFile> affectedOutputPaths) {
        StringBuilder paths = new StringBuilder();
        for (VirtualFile affectedOutputPath : affectedOutputPaths) {
            if (paths.length() > 0) {
                paths.append(",\n");
            }
            paths.append(affectedOutputPath.getPath().replace('/', File.separatorChar));
        }
        int answer = Messages.showOkCancelDialog(project,
            CompilerLocalize.warningSourcesUnderOutputPaths(paths.toString()).get(),
            CommonLocalize.titleError().get(),
            UIUtil.getWarningIcon()
        );
        return answer == Messages.OK;
    }
}
