// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.function.Processors;
import consulo.content.scope.SearchScope;
import consulo.index.io.ID;
import consulo.language.internal.FileNameIndexService;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author yole
 */
public class FilenameIndex {
    /**
     * @deprecated Not to be used.
     */
    @Deprecated
    public static final ID<String, Void> NAME = ID.create("FilenameIndex");

    @Nonnull
    public static String[] getAllFilenames(@Nullable Project project) {
        Set<String> names = new HashSet<>();
        getService().processAllFileNames(
            (String s) -> {
                names.add(s);
                return true;
            },
            project == null ? new EverythingGlobalScope() : GlobalSearchScope.allScope(project),
            null
        );
        return ArrayUtil.toStringArray(names);
    }

    public static void processAllFileNames(
        @Nonnull Predicate<? super String> processor,
        @Nonnull SearchScope scope,
        @Nullable IdFilter filter
    ) {
        getService().processAllFileNames(processor, scope, filter);
    }

    @Nonnull
    public static Collection<VirtualFile> getVirtualFilesByName(Project project, @Nonnull String name, @Nonnull SearchScope scope) {
        return getService().getVirtualFilesByName(project, name, scope, null);
    }

    @Nonnull
    public static Collection<VirtualFile> getVirtualFilesByName(
        Project project,
        @Nonnull String name,
        boolean caseSensitively,
        @Nonnull GlobalSearchScope scope
    ) {
        if (caseSensitively) {
            return getVirtualFilesByName(project, name, scope);
        }
        return getVirtualFilesByNameIgnoringCase(name, scope, project, null);
    }

    @Nonnull
    @RequiredReadAction
    public static PsiFile[] getFilesByName(@Nonnull Project project, @Nonnull String name, @Nonnull SearchScope scope) {
        return (PsiFile[])getFilesByName(project, name, scope, false);
    }

    @RequiredReadAction
    public static boolean processFilesByName(
        @Nonnull String name,
        boolean directories,
        @Nonnull Predicate<? super PsiFileSystemItem> processor,
        @Nonnull SearchScope scope,
        @Nonnull Project project,
        @Nullable IdFilter idFilter
    ) {
        return processFilesByName(name, directories, true, processor, scope, project, idFilter);
    }

    @RequiredReadAction
    public static boolean processFilesByName(
        @Nonnull String name,
        boolean directories,
        boolean caseSensitively,
        @Nonnull Predicate<? super PsiFileSystemItem> processor,
        @Nonnull SearchScope scope,
        @Nonnull Project project,
        @Nullable IdFilter idFilter
    ) {
        Collection<VirtualFile> files;

        if (caseSensitively) {
            files = getService().getVirtualFilesByName(project, name, scope, idFilter);
        }
        else {
            files = getVirtualFilesByNameIgnoringCase(name, scope, project, idFilter);
        }

        if (files.isEmpty()) {
            return false;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        int processedFiles = 0;

        for (VirtualFile file : files) {
            if (!file.isValid()) {
                continue;
            }
            if (!directories && !file.isDirectory()) {
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile != null) {
                    if (!processor.test(psiFile)) {
                        return true;
                    }
                    ++processedFiles;
                }
            }
            else if (directories && file.isDirectory()) {
                PsiDirectory dir = psiManager.findDirectory(file);
                if (dir != null) {
                    if (!processor.test(dir)) {
                        return true;
                    }
                    ++processedFiles;
                }
            }
        }
        return processedFiles > 0;
    }

    @Nonnull
    private static Set<VirtualFile> getVirtualFilesByNameIgnoringCase(
        @Nonnull String name,
        @Nonnull SearchScope scope,
        @Nonnull Project project,
        @Nullable IdFilter idFilter
    ) {
        Set<String> keys = new HashSet<>();
        FileNameIndexService fileNameIndexService = getService();
        fileNameIndexService.processAllFileNames(value -> {
            if (name.equalsIgnoreCase(value)) {
                keys.add(value);
            }
            return true;
        }, scope, idFilter);

        // values accessed outside of processAllKeys
        Set<VirtualFile> files = new HashSet<>();
        for (String each : keys) {
            files.addAll(fileNameIndexService.getVirtualFilesByName(project, each, scope, idFilter));
        }
        return files;
    }

    @Nonnull
    @RequiredReadAction
    public static PsiFileSystemItem[] getFilesByName(
        @Nonnull Project project,
        @Nonnull String name,
        @Nonnull SearchScope scope,
        boolean directories
    ) {
        SmartList<PsiFileSystemItem> result = new SmartList<>();
        Predicate<PsiFileSystemItem> processor = Processors.cancelableCollectProcessor(result);
        processFilesByName(name, directories, processor, scope, project, null);

        if (directories) {
            return result.toArray(new PsiFileSystemItem[0]);
        }
        //noinspection SuspiciousToArrayCall
        return result.toArray(PsiFile.EMPTY_ARRAY);
    }

    /**
     * Returns all files in the project by extension
     *
     * @param project current project
     * @param ext     file extension without leading dot e.q. "txt", "wsdl"
     * @return all files with provided extension
     * @author Konstantin Bulenkov
     */
    @Nonnull
    public static Collection<VirtualFile> getAllFilesByExt(@Nonnull Project project, @Nonnull String ext) {
        return getAllFilesByExt(project, ext, GlobalSearchScope.allScope(project));
    }

    @Nonnull
    public static Collection<VirtualFile> getAllFilesByExt(
        @Nonnull Project project,
        @Nonnull String ext,
        @Nonnull GlobalSearchScope searchScope
    ) {
        int len = ext.length();

        if (len == 0) {
            return Collections.emptyList();
        }

        ext = "." + ext;
        len++;

        List<VirtualFile> files = new ArrayList<>();
        for (String name : getAllFilenames(project)) {
            int length = name.length();
            if (length > len && name.substring(length - len).equalsIgnoreCase(ext)) {
                files.addAll(getVirtualFilesByName(project, name, searchScope));
            }
        }
        return files;
    }

    static FileNameIndexService getService() {
        return Application.get().getInstance(FileNameIndexService.class);
    }
}
